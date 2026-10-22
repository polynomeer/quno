#!/usr/bin/env python3
"""실험 행렬을 무작위 순서로 실행한다 (이미 결과가 있으면 건너뜀 = 재개 가능).

사용:
  scripts/run-matrix.py --pilot                 # 비용 측정용 5회
  scripts/run-matrix.py [--runs 3] [--l-runs 1] [--only A03,C07] [--conds A0,A1,A2,A3a,A3b] [--budget 3] [--max-total-usd 200]
조건 A3a/A3b는 같은 미끼 코퍼스에서 라벨 표시 여부만 다르다. B09는 정답·미끼 기록이 없어 A2/A3 조건에서 제외한다.
"""
import argparse, json, os, random, subprocess, sys, glob
HERE = os.path.dirname(os.path.abspath(__file__)); TASKS = os.path.dirname(HERE)
RESULTS = os.path.join(TASKS, "runs")
ALL = [f"A{i:02d}" for i in range(1, 11)] + [f"B{i:02d}" for i in range(1, 11)] + [f"C{i:02d}" for i in range(1, 11)]
L = {"B07", "B08", "B09", "B10"}
PILOT = [("C07", "A0", 1), ("A03", "A0", 1), ("A03", "A2", 1), ("A01", "A1", 1), ("B07", "A0", 1)]

ap = argparse.ArgumentParser()
ap.add_argument("--pilot", action="store_true"); ap.add_argument("--runs", type=int, default=3); ap.add_argument("--l-runs", type=int, default=1)
ap.add_argument("--only"); ap.add_argument("--conds", default="A0,A1,A2,A3a,A3b"); ap.add_argument("--budget", type=float, default=3.0)
ap.add_argument("--max-total-usd", type=float, default=0); ap.add_argument("--model"); ap.add_argument("--timeout", type=int, default=1500)
a = ap.parse_args()

if a.pilot:
    jobs = PILOT
else:
    tasks = a.only.split(",") if a.only else ALL
    conds = a.conds.split(",")
    jobs = []
    for t in tasks:
        for c in conds:
            if t == "B09" and c in ("A2", "A3a", "A3b"): continue
            for n in range(1, (a.l_runs if t in L else a.runs) + 1): jobs.append((t, c, n))
    random.Random(91).shuffle(jobs)

def spent():
    tot = 0.0
    for f in glob.glob(os.path.join(RESULTS, "*.json")):
        try: tot += json.load(open(f, encoding="utf-8")).get("total_cost_usd") or 0
        except Exception: pass
    return tot

done = skipped = invalid_streak = 0
for t, c, n in jobs:
    rid = f"{t}-{c}-r{n}"
    if os.path.exists(os.path.join(RESULTS, rid + ".json")):
        skipped += 1; continue
    if a.max_total_usd and spent() >= a.max_total_usd:
        print(f"누적 비용 {spent():.2f} USD 가 상한 {a.max_total_usd} 에 도달 — 중단 (다시 실행하면 이어서 진행)"); break
    cmd = [sys.executable, os.path.join(HERE, "run-one.py"), t, c, str(n), "--budget", str(a.budget), "--timeout", str(a.timeout)]
    if a.model: cmd += ["--model", a.model]
    p = subprocess.run(cmd, capture_output=True, text=True)
    print(p.stdout.strip() or p.stderr.strip()[-300:], flush=True)
    if p.returncode == 3:
        invalid_streak += 1
        if invalid_streak >= 2:
            print("연속 2회 무효 실행(인증·네트워크 문제 가능) — 중단합니다. 해결 후 다시 실행하면 이어서 진행합니다."); sys.exit(3)
        continue
    invalid_streak = 0
    done += 1
print(f"완료 {done}건, 건너뜀(기존 결과) {skipped}건, 누적 비용 {spent():.2f} USD")
