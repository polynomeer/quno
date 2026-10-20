#!/usr/bin/env bash
# 알려진 정답(answers/<ID>.patch)을 적용하면 CHECK가 통과하는지 확인한다 — 과제가 풀 수 있는 문제임을 보증한다.
set -uo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
id="${1:?usage: verify-solution.sh <TASK_ID>}"
dir="$("$here/scripts/prepare.sh" "$id")"
patch="$here/answers/$id.patch"
[ -f "$patch" ] || { echo "no patch: $patch" >&2; exit 2; }
( cd "$dir" && patch -p1 -s < "$patch" ) || { echo "FAIL[$id]: 패치 적용 실패"; exit 1; }
log="$dir/solution.log"
( cd "$dir" && bash ./CHECK ) >"$log" 2>&1
code=$?
[ $code -eq 0 ] && { echo "OK[$id]: 정답 적용 시 통과 — $log"; exit 0; }
echo "FAIL[$id]: 정답을 적용해도 CHECK 실패 (exit=$code) — $log"; exit 1
