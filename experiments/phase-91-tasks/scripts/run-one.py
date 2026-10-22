#!/usr/bin/env python3
"""과제 하나를 한 조건으로 코딩 에이전트(claude -p)에게 풀게 하고 결과를 JSON으로 남긴다.

사용: scripts/run-one.py <TASK_ID> <A0|A1|A2|A3a|A3b> <RUN_NO> [--model M] [--budget USD] [--timeout SEC] [--dry-run]

격리 규칙
- 에이전트의 작업 디렉터리는 scripts/prepare.sh 가 만든 복사본(answers 미포함)이다.
- 코퍼스(정답·미끼 포함)는 작업 디렉터리 밖의 별도 경로에 두고 MCP 서버만 읽는다.
- MCP는 --strict-mcp-config 로 이 서버(또는 A0이면 없음)만 쓴다. 도구는 --allowedTools 목록 외에는 거부(dontAsk).
- 프롬프트는 모든 조건에서 동일하다. Quno 도구를 쓰라는 문구가 없다 — 스스로 호출하는지가 측정 대상(H1).
"""
import argparse, json, os, shutil, subprocess, sys, time

HERE = os.path.dirname(os.path.abspath(__file__))
TASKS = os.path.dirname(HERE)
REPO = os.path.abspath(os.path.join(TASKS, "..", ".."))
MCP = os.path.join(REPO, "experiments", "phase-91-mcp")
RUNS_ROOT = os.environ.get("QUNO_RUNS_ROOT", "/tmp/quno-runs")
PRIVATE_ROOT = RUNS_ROOT + "_private"  # 코퍼스·MCP 설정·호출 로그. 에이전트의 작업 디렉터리 밖
RESULTS = os.path.join(TASKS, "runs")

COND_CORPUS = {"A1": "A1", "A2": "A2", "A3a": "A3", "A3b": "A3"}

PROMPT = """당신은 현재 디렉터리의 소프트웨어 프로젝트에서 일하고 있습니다. 다음 문제가 보고되었습니다.

{task}

스스로 판단해 끝까지 진행하세요. 질문하지 마세요. 끝나면 근본 원인과 바꾼 내용을 짧게 요약해 답하세요."""

# 파일 도구는 작업 디렉터리로 한정하고, 임의 경로를 읽는 셸 명령(cat/find/ls ...)은 허용하지 않는다.
ALLOWED = [
    "Read(./**)", "Edit(./**)", "Write(./**)", "Glob(./**)", "Grep(./**)",
    "Bash(./gradlew:*)", "Bash(./CHECK:*)", "Bash(bash ./CHECK:*)",
]
MCP_ALLOWED = ["mcp__quno__search_known_issues", "mcp__quno__get_resolution"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("task"); ap.add_argument("cond", choices=["A0", "A1", "A2", "A3a", "A3b"]); ap.add_argument("run", type=int)
    ap.add_argument("--model"); ap.add_argument("--budget", type=float, default=3.0)
    ap.add_argument("--timeout", type=int, default=1500); ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()

    run_id = f"{a.task}-{a.cond}-r{a.run}"
    base = os.path.join(RUNS_ROOT, run_id)          # 작업 디렉터리만 둔다
    priv = os.path.join(PRIVATE_ROOT, run_id)       # 비공개 파일
    shutil.rmtree(base, ignore_errors=True); shutil.rmtree(priv, ignore_errors=True)
    os.makedirs(priv)
    work = os.path.join(base, "work")
    subprocess.run([os.path.join(HERE, "prepare.sh"), a.task, work], check=True, stdout=subprocess.DEVNULL)
    task_md = open(os.path.join(work, "TASK.md"), encoding="utf-8").read()

    cmd = ["claude", "-p", PROMPT.format(task=task_md), "--output-format", "json",
           "--permission-mode", "dontAsk", "--no-session-persistence", "--disable-slash-commands",
           "--max-budget-usd", str(a.budget), "--strict-mcp-config"]
    allowed = list(ALLOWED)
    calls_log = os.path.join(priv, "calls.jsonl")
    if a.cond == "A0":
        cfg = {"mcpServers": {}}
    else:
        corp_src = os.path.join(MCP, "corpora", "build", COND_CORPUS[a.cond])
        if not os.path.isdir(corp_src):
            sys.exit("corpora/build 가 없다: experiments/phase-91-mcp/build-corpora.sh 를 먼저 실행")
        corp = os.path.join(priv, "corpus")
        shutil.copytree(corp_src, corp)
        env = {"QUNO_SEED_DIR": corp, "QUNO_CALL_LOG": calls_log, "QUNO_CONDITION": a.cond, "QUNO_RUN_ID": run_id}
        if a.cond == "A3b":
            env["QUNO_STRIP_LABELS"] = "1"
        cfg = {"mcpServers": {"quno": {"command": "node", "args": [os.path.join(MCP, "src", "server.js")], "env": env}}}
        allowed += MCP_ALLOWED
    cfg_path = os.path.join(priv, "mcp.json")
    json.dump(cfg, open(cfg_path, "w"))
    cmd += ["--mcp-config", cfg_path, "--allowedTools", ",".join(allowed)]
    if a.model:
        cmd += ["--model", a.model]

    if a.dry_run:
        print(json.dumps({"run_id": run_id, "work": work, "cmd": cmd[:3] + ["<prompt>"] + cmd[4:]}, ensure_ascii=False, indent=1))
        return

    t0 = time.time()
    timed_out = False
    try:
        p = subprocess.run(cmd, cwd=work, capture_output=True, text=True, timeout=a.timeout)
        out, err, code = p.stdout, p.stderr, p.returncode
    except subprocess.TimeoutExpired as e:
        out, err, code, timed_out = (e.stdout or b"").decode() if isinstance(e.stdout, bytes) else (e.stdout or ""), "timeout", -1, True
    wall = time.time() - t0

    agent = {}
    try:
        agent = json.loads(out)
    except Exception:
        pass

    # 독립 판정: 에이전트의 보고가 아니라 CHECK 의 종료 코드
    c0 = time.time()
    try:
        chk = subprocess.run(["bash", "./CHECK"], cwd=work, capture_output=True, text=True, timeout=900)
        check_code, check_tail = chk.returncode, (chk.stdout + chk.stderr)[-600:]
    except subprocess.TimeoutExpired:
        check_code, check_tail = -1, "CHECK timeout"
    check_wall = time.time() - c0

    calls = []
    if os.path.exists(calls_log):
        calls = [json.loads(l) for l in open(calls_log, encoding="utf-8") if l.strip()]
    diag = os.path.join(work, "DIAGNOSIS.md")

    res = {
        "run_id": run_id, "task": a.task, "cond": a.cond, "run": a.run,
        "agent_exit": code, "timed_out": timed_out, "wall_s": round(wall, 1),
        "check_exit": check_code, "check_wall_s": round(check_wall, 1), "check_tail": check_tail,
        "solved": check_code == 0,
        "quno_calls": len([c for c in calls if c.get("tool") == "search_known_issues"]),
        "quno_resolution_calls": len([c for c in calls if c.get("tool") == "get_resolution"]),
        "returned_ids": sorted({i for c in calls for i in (c.get("resultIds") or [])}),
        "total_cost_usd": agent.get("total_cost_usd"), "num_turns": agent.get("num_turns"),
        "duration_ms": agent.get("duration_ms"), "usage": agent.get("usage"), "modelUsage": agent.get("modelUsage"),
        "is_error": agent.get("is_error"), "result": agent.get("result"),
        "diagnosis_md": open(diag, encoding="utf-8").read() if os.path.exists(diag) else None,
        "stderr_tail": (err or "")[-400:],
    }
    # 에이전트가 실제로 일하지 못한 실행(인증 실패 등)은 데이터가 아니다: runs/_invalid/ 에 두고 종료 코드 3.
    invalid = (not agent) or agent.get("is_error") or not agent.get("num_turns") or (agent.get("total_cost_usd") in (None, 0) and not agent.get("usage"))
    if invalid:
        os.makedirs(os.path.join(RESULTS, "_invalid"), exist_ok=True)
        json.dump(res, open(os.path.join(RESULTS, "_invalid", run_id + ".json"), "w"), ensure_ascii=False, indent=1)
        print(json.dumps({"run_id": run_id, "invalid": True, "reason": (agent.get("result") or err or "no output")[:200]}, ensure_ascii=False))
        sys.exit(3)
    os.makedirs(RESULTS, exist_ok=True)
    json.dump(res, open(os.path.join(RESULTS, run_id + ".json"), "w"), ensure_ascii=False, indent=1)
    shutil.copy(calls_log, os.path.join(RESULTS, run_id + ".calls.jsonl")) if os.path.exists(calls_log) else None
    print(json.dumps({k: res[k] for k in ("run_id", "solved", "wall_s", "total_cost_usd", "num_turns", "quno_calls", "returned_ids", "timed_out")}, ensure_ascii=False))


if __name__ == "__main__":
    main()
