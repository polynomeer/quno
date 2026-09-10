#!/usr/bin/env bash
# db-backup.sh로 만든 백업 파일을 복구한다(production-readiness.md B-4). **대상 데이터베이스를
# 덮어쓰는 파괴적인 작업**이라 실행 전 확인을 요구한다. 복구 절차가 실제로 동작하는지 정기적으로
# 리허설하는 용도로도 쓴다(백업만 하고 복구를 검증하지 않으면 막상 필요할 때 실패하는 경우가 흔하다).
#
# 사용법:
#   ./scripts/db-restore.sh backups/quno-20260910-120000.dump
#   PGHOST=... ./scripts/db-restore.sh backups/quno-20260910-120000.dump   # 원격 DB로 복구

set -euo pipefail

BACKUP_FILE="${1:?사용법: $0 <백업 파일 경로>}"
[[ -f "$BACKUP_FILE" ]] || { echo "백업 파일을 찾을 수 없습니다: $BACKUP_FILE" >&2; exit 1; }

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

TARGET_DESC="${PGHOST:-로컬 docker-compose postgres 컨테이너}"
echo "!!! 경고: $TARGET_DESC 의 quno 데이터베이스를 '$BACKUP_FILE' 내용으로 덮어씁니다 !!!"
read -r -p "계속하려면 'yes'를 입력하세요: " CONFIRM
[[ "$CONFIRM" == "yes" ]] || { echo "취소되었습니다."; exit 1; }

if [[ -n "${PGHOST:-}" ]]; then
  pg_restore --clean --if-exists --no-owner --dbname="${PGDATABASE:-quno}" "$BACKUP_FILE"
else
  cd "$ROOT_DIR"
  docker compose exec -T postgres pg_restore -U quno --clean --if-exists --no-owner -d quno < "$BACKUP_FILE"
fi

echo "복구 완료"
