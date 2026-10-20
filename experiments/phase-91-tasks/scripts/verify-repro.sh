#!/usr/bin/env bash
# 수정 전 버전에서 증상이 실제로 재현되는지 확인한다 (과제 확정의 필수 조건).
# tasks/<ID>/CHECK(성공이면 종료 코드 0인 스크립트)를 실행해 "실패"하고, answers/<ID>.signature 의 문자열이 출력에 있어야 통과.
set -uo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
id="${1:?usage: verify-repro.sh <TASK_ID>}"
dir="$("$here/scripts/prepare.sh" "$id")"
sig="$(cat "$here/answers/$id.signature")"
log="$dir/verify.log"
( cd "$dir" && bash ./CHECK ) >"$log" 2>&1
code=$?
if grep -qE "(compile[A-Za-z]*|:compile[A-Za-z]*) FAILED|error: |^e: " "$log"; then echo "FAIL[$id]: 컴파일 오류 — 증상이 아니라 과제 코드 문제 ($log)"; exit 1; fi
if [ $code -eq 0 ]; then echo "FAIL[$id]: 증상이 재현되지 않음 (명령이 성공함) — $log"; exit 1; fi
if grep -qF -- "$sig" "$log"; then echo "OK[$id]: 증상 재현 확인 (exit=$code, signature 일치) — $log"; exit 0; fi
echo "FAIL[$id]: 실패했지만 기대한 signature가 아님 (exit=$code) — $log"; echo "기대: $sig"; exit 1
