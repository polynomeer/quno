#!/usr/bin/env bash
# 과제 디렉터리를 에이전트에게 줄 작업 디렉터리로 복사한다.
# 정답(answers/)과 저장소 이력은 포함하지 않는다 — 에이전트가 읽을 수 없는 위치에서 실행해야 한다.
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
id="${1:?usage: prepare.sh <TASK_ID> [dest_dir]}"
dest="${2:-$(mktemp -d "${TMPDIR:-/tmp}/quno-task-$id.XXXXXX")}"
[ -d "$here/tasks/$id" ] || { echo "no such task: $id" >&2; exit 2; }
mkdir -p "$dest"
cp -R "$here/tasks/$id/." "$dest/"
cp -R "$here/_common/wrapper/." "$dest/"
chmod +x "$dest/gradlew"
echo "$dest"
