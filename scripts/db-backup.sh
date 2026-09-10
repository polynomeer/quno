#!/usr/bin/env bash
# PostgreSQL 백업 스크립트(production-readiness.md B-4). 기본은 로컬 docker-compose의
# postgres 컨테이너를 대상으로 하고, PGHOST(표준 libpq 환경변수)가 설정돼 있으면 그 대상으로
# 로컬 pg_dump를 바로 실행한다 — 운영 DB를 이 스크립트로 백업할 때 쓰는 방식이다. 주기 실행(cron
# 등)과 백업 파일의 실제 보관(오프사이트 저장소 등)은 인프라를 구성하는 사람의 몫이다.
#
# 사용법:
#   ./scripts/db-backup.sh                                     # 로컬 docker-compose 컨테이너 백업
#   PGHOST=prod-db.example.com PGUSER=quno ./scripts/db-backup.sh   # 원격 DB 백업(비밀번호는 PGPASSWORD 또는 ~/.pgpass)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUT_FILE="$BACKUP_DIR/quno-$TIMESTAMP.dump"

if [[ -n "${PGHOST:-}" ]]; then
  echo "PGHOST=$PGHOST 대상으로 pg_dump 실행..."
  pg_dump --format=custom --dbname="${PGDATABASE:-quno}" --file="$OUT_FILE"
else
  echo "로컬 docker-compose postgres 컨테이너 대상으로 pg_dump 실행..."
  cd "$ROOT_DIR"
  docker compose exec -T postgres pg_dump -U quno --format=custom quno > "$OUT_FILE"
fi

echo "백업 완료: $OUT_FILE ($(du -h "$OUT_FILE" | cut -f1))"
