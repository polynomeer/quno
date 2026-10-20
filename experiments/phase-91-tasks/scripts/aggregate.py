#!/usr/bin/env python3
"""runs/*.json 을 조건·층별로 집계한다. 사용: scripts/aggregate.py [runs_dir]

- R/D 과제(B07~B10 L 제외)만 해결률·시간·비용으로 집계한다. L 과제는 루브릭 점수로 따로 본다.
- 호출률 = Quno 도구를 1회 이상 호출한 실행의 비율 (A1/A2/A3).
- 쌍체 시간 단축 = 과제별 (A0 중앙값 - 조건 중앙값) / A0 중앙값, 두 조건 모두에서 해결된 실행만.
"""
import json, os, sys, glob, statistics as st
from collections import defaultdict
L_TASKS = {"B07", "B08", "B09", "B10"}
d = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "runs")
runs = [json.load(open(f, encoding="utf-8")) for f in sorted(glob.glob(os.path.join(d, "*.json"))) if not f.endswith(".calls.jsonl")]
runs = [r for r in runs if "run_id" in r]
def med(xs): xs = [x for x in xs if x is not None]; return round(st.median(xs), 2) if xs else None
def layer(t): return t[0]
print(f"실행 {len(runs)}건 (L 과제 {sum(r['task'] in L_TASKS for r in runs)}건은 별도 채점)\n")
by = defaultdict(list)
for r in runs:
    if r["task"] in L_TASKS: continue
    by[(r["cond"], layer(r["task"]))].append(r)
    by[(r["cond"], "A+B")].append(r) if layer(r["task"]) in "AB" else None
print("| 조건 | 층 | n | 해결률 | 시간 중앙값(s) | 비용 중앙값($) | 턴 중앙값 | 호출률 |\n|---|---|---|---|---|---|---|---|")
for (c, l), xs in sorted(by.items()):
    called = sum(1 for x in xs if x["quno_calls"] > 0) / len(xs) if c != "A0" else None
    print(f"| {c} | {l} | {len(xs)} | {sum(x['solved'] for x in xs)/len(xs):.0%} | {med([x['wall_s'] for x in xs])} | {med([x['total_cost_usd'] for x in xs])} | {med([x['num_turns'] for x in xs])} | {'-' if called is None else format(called,'.0%')} |")
# 쌍체 시간 단축 (A+B)
base = defaultdict(list)
for r in runs:
    if r["cond"] == "A0" and r["solved"] and r["task"] not in L_TASKS: base[r["task"]].append(r["wall_s"])
print("\n쌍체 시간 단축 (A0 대비, 둘 다 해결한 과제, A·B층):")
for c in ("A1", "A2", "A3a", "A3b"):
    red = []
    for t, bs in base.items():
        if layer(t) not in "AB": continue
        xs = [r["wall_s"] for r in runs if r["task"] == t and r["cond"] == c and r["solved"]]
        if xs: red.append((st.median(bs) - st.median(xs)) / st.median(bs))
    print(f"  {c}: {'-' if not red else format(st.median(red),'.0%')} (과제 {len(red)}개)")
print("\n미끼 조건에서 해결 실패 + 미끼 기록을 받은 실행(오염 후보, 수동 확인 필요):")
for r in runs:
    if r["cond"] in ("A3a", "A3b") and not r["solved"] and any(i.startswith("decoy-") for i in r["returned_ids"]):
        print("  ", r["run_id"], [i for i in r["returned_ids"] if i.startswith("decoy-")])
