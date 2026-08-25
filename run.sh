#!/usr/bin/env bash
# Runs the whole Quno backend project locally: brings up Docker infra
# (PostgreSQL/MongoDB/Redis) and then starts the Spring Boot server in the
# foreground with the `local` profile.
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

step "2. 인프라 기동 (PostgreSQL/MongoDB/Redis)"
docker compose up -d

step "3. PostgreSQL 준비 대기"
until docker compose exec -T postgres pg_isready -U quno >/dev/null 2>&1; do
  echo -n "."
  sleep 1
done
echo
echo "PostgreSQL 준비 완료"

step "4. 백엔드 서버 기동 (local 프로필, http://localhost:8081)"
echo "종료하려면 Ctrl+C를 누르세요. 기동 후 http://localhost:8081/actuator/health 에서 상태를 확인할 수 있습니다."
cd "$ROOT_DIR/backend"
export SPRING_PROFILES_ACTIVE=local
exec ./gradlew bootRun
