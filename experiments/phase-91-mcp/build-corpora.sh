#!/usr/bin/env bash
# 조건별 코퍼스 디렉터리를 만든다: A1(시드), A2(시드+정답 기록), A3(시드+미끼). 결과는 corpora/build/ (gitignore).
# 에이전트가 읽을 수 있는 위치에 정답/미끼 원본(corpora/oracle, corpora/decoy)이 노출되지 않도록,
# 실험 때는 build 결과를 에이전트가 접근하지 못하는 경로로 복사해 QUNO_SEED_DIR 로 지정한다.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
out="$here/corpora/build"
rm -rf "$out"; mkdir -p "$out/A1" "$out/A2" "$out/A3"
cp "$here"/seeds/seed-chunk*.json "$out/A1/"
cp "$here"/seeds/seed-chunk*.json "$out/A2/"; cp "$here"/corpora/oracle/*.json "$out/A2/" 2>/dev/null || true
cp "$here"/seeds/seed-chunk*.json "$out/A3/"; cp "$here"/corpora/decoy/*.json "$out/A3/" 2>/dev/null || true
for c in A1 A2 A3; do echo "$c: $(ls "$out/$c"/*.json | wc -l | tr -d ' ') files"; node "$here/src/validate-seeds.js" "$out/$c" | tail -1; done
