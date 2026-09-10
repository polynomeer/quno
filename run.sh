#!/usr/bin/env bash
# Runs the whole Quno project locally: brings up Docker infra
# (PostgreSQL/MongoDB/Redis/Mailpit), then starts the Spring Boot backend
# (local profile) and the Next.js frontend dev server together.
#
# Usage: ./run.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

step() { printf '\n\033[1;36m=== %s ===\033[0m\n' "$1"; }

step "1. Docker 데몬 확인"
if ! docker info >/dev/null 2>&1; then
  echo "Docker가 실행 중이 아닙니다."
  if [[ "$(uname)" == "Darwin" ]]; then
    echo "Docker Desktop을 실행합니다..."
    open -a Docker
    echo -n "Docker 데몬 기동 대기 중"
    until docker info >/dev/null 2>&1; do
      echo -n "."
      sleep 2
    done
    echo
  else
    echo "Docker를 먼저 실행한 뒤 다시 시도해 주세요." >&2
    exit 1
  fi
fi
echo "Docker 데몬 준비 완료"

step "2. 인프라 기동 (PostgreSQL/MongoDB/Redis/Mailpit)"
docker compose up -d

step "3. PostgreSQL 준비 대기"
until docker compose exec -T postgres pg_isready -U quno >/dev/null 2>&1; do
  echo -n "."
  sleep 1
done
echo
echo "PostgreSQL 준비 완료"

step "4. 프론트엔드 의존성 확인"
if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
  echo "node_modules가 없어 설치합니다 (최초 1회)..."
  (cd "$ROOT_DIR/frontend" && npm install)
fi
echo "프론트엔드 의존성 준비 완료"

# 백엔드/프론트엔드를 백그라운드 잡으로 띄우고 Ctrl+C 한 번으로 둘 다 종료한다 — 원래 스크립트의
# `exec ./gradlew bootRun`은 셸 프로세스를 통째로 대체해버려 두 번째 프로세스를 함께 관리할 수 없다.
# `$!`로 잡히는 PID는 `gradlew`/`npm` 래퍼 프로세스이지 실제 포트를 여는 자바/노드 프로세스가
# 아니다(둘 다 자식 프로세스를 새로 fork한다) — 그 PID만 죽이면 래퍼는 죽어도 실제 서버는 고아
# 프로세스로 남는다. 포트를 실제로 열고 있는 프로세스를 직접 찾아 종료해야 확실하다.
cleanup() {
  echo
  echo "종료 중..."
  local backend_listener frontend_listener
  backend_listener=$(lsof -ti :8081 -sTCP:LISTEN 2>/dev/null || true)
  frontend_listener=$(lsof -ti :3000 -sTCP:LISTEN 2>/dev/null || true)
  [[ -n "$backend_listener" ]] && kill $backend_listener >/dev/null 2>&1
  [[ -n "$frontend_listener" ]] && kill $frontend_listener >/dev/null 2>&1
  kill "$BACKEND_PID" "$FRONTEND_PID" >/dev/null 2>&1 || true
  wait "$BACKEND_PID" "$FRONTEND_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

step "5. 백엔드 서버 기동 (local 프로필, http://localhost:8081)"
# `--no-daemon`: Gradle Daemon을 쓰면 실제 JVM이 run.sh와 무관한 상주 데몬 프로세스의
# 자식으로 뜬다(부모가 run.sh 프로세스 그룹 밖에 있음) — Ctrl+C가 그룹 전체에 신호를 보내도
# 이 JVM은 받지 못해 고아로 남는다. --no-daemon으로 JVM을 run.sh의 직계 자손으로 유지한다.
(
  cd "$ROOT_DIR/backend"
  export SPRING_PROFILES_ACTIVE=local
  ./gradlew --no-daemon bootRun
) &
BACKEND_PID=$!

step "6. 프론트엔드 개발 서버 기동 (http://localhost:3000)"
(
  cd "$ROOT_DIR/frontend"
  npm run dev
) &
FRONTEND_PID=$!

echo
echo "백엔드:    http://localhost:8081  (상태 확인: http://localhost:8081/actuator/health)"
echo "프론트엔드: http://localhost:3000"
echo "종료하려면 Ctrl+C를 누르세요."

wait "$BACKEND_PID" "$FRONTEND_PID"
