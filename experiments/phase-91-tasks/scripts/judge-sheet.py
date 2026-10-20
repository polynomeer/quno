#!/usr/bin/env python3
"""적중 판정용 시트를 만든다 (조건을 가림). 사용: scripts/judge-sheet.py [runs_dir]

각 실행이 반환받은 기록을 익명 라벨(R1, R2, ...)로 보여 주고, 평가자는 과제의 정답(answers/<ID>.md의 원인)과 비교해
관련도(2=같은 원인, 1=같은 실패 계열의 실행 가능한 단서, 0=무관)를 매긴다.
- runs/judge-sheet.md : 평가자에게 주는 시트(조건·기록 id 없음)
- runs/judge-key.json : 익명 라벨 → (run_id, 기록 id, 조건). 평가자에게 주지 않는다.
"""
import json, os, sys, glob, random, re
here = os.path.dirname(os.path.abspath(__file__))
d = sys.argv[1] if len(sys.argv) > 1 else os.path.join(here, "..", "runs")
mcp = os.path.join(here, "..", "..", "phase-91-mcp", "corpora", "build")
def corpus(cond):
    c = {"A1": "A1", "A2": "A2", "A3a": "A3", "A3b": "A3"}[cond]
    recs = {}
    for f in glob.glob(os.path.join(mcp, c, "*.json")):
        for r in json.load(open(f, encoding="utf-8")): recs[r["id"]] = r
    return recs
runs = [json.load(open(f, encoding="utf-8")) for f in sorted(glob.glob(os.path.join(d, "*.json"))) if "calls" not in f and "judge" not in f]
runs = [r for r in runs if r.get("cond") not in (None, "A0") and r.get("returned_ids")]
random.seed(91); random.shuffle(runs)
out = ["# 적중 판정 시트 (조건 비공개)\n", "관련도: 2 = 같은 근본 원인 / 1 = 같은 실패 계열의 실행 가능한 단서 / 0 = 무관. 기록 하나씩, 정답과 비교해 매긴다.\n"]
key = {}
for i, r in enumerate(runs, 1):
    ans = open(os.path.join(here, "..", "answers", r["task"] + ".md"), encoding="utf-8").read()
    cause = re.search(r"(근본 원인|원인)[:：]?\s*(.+)", ans)
    out.append(f"\n## 시트 {i} — 과제 {r['task']}\n정답(원인): {cause.group(2).strip()[:400] if cause else '(answers 참고)'}\n")
    recs = corpus(r["cond"])
    for j, rid in enumerate(r["returned_ids"], 1):
        rec = recs.get(rid)
        label = f"S{i}-R{j}"
        key[label] = {"run_id": r["run_id"], "record": rid, "cond": r["cond"]}
        if rec:
            out.append(f"- **{label}** {rec['title']}\n  - symptom: {rec['symptom']}\n  - cause: {rec['cause']}\n  - fix: {rec['fixSummary']}\n  - 관련도: ___\n")
open(os.path.join(d, "judge-sheet.md"), "w", encoding="utf-8").write("\n".join(out))
json.dump(key, open(os.path.join(d, "judge-key.json"), "w"), indent=1)
print(f"시트 {len(runs)}건, 기록 {len(key)}개 -> {d}/judge-sheet.md")
