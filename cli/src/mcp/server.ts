import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { QunoApiError, type QunoClient } from "../client.js";
import { OmitPathError } from "../snapshot/compose.js";
import { captureEnvironment, getMyStack, getQuestion, searchKnownIssues, searchQuestions, type LocalEnvironment, type ToolOutput } from "./tools.js";

const VERSION = "0.1.0";
const readOnly = { readOnlyHint: true, openWorldHint: true } as const;

async function run(produce: () => Promise<ToolOutput>) {
  try {
    const output = await produce();
    return { content: [{ type: "text" as const, text: output.json }], structuredContent: output.value };
  } catch (error) {
    const message =
      error instanceof QunoApiError ? `Quno API ${error.status} ${error.code}: ${error.message}` : error instanceof OmitPathError ? error.message : String(error);
    return { content: [{ type: "text" as const, text: message }], isError: true };
  }
}

/**
 * Read-only tools over the Quno API (Phase 65.3, ADR-0080) — a READ-scoped token is enough — plus
 * `capture_environment` (Phase 93.5, ADR-0084), which reads this machine and sends nothing.
 */
export function createServer(client: QunoClient, webUrl: string, local?: LocalEnvironment): McpServer {
  const server = new McpServer({ name: "quno", version: VERSION });

  server.registerTool(
    "search_known_issues",
    {
      title: "Search known issues",
      description:
        "빌드·실행·테스트에서 에러가 났을 때 같은 에러를 먼저 겪은 질문이 Quno에 있는지 찾는다. " +
        "에러 메시지나 스택트레이스 전체를 그대로 넣는다(경로·줄 번호는 서버가 정규화한다). " +
        "같은 근본 원인(예외 클래스 + 상위 프레임)을 가진 질문이 먼저 나온다.",
      inputSchema: {
        errorText: z.string().min(1).max(100_000).describe("에러 메시지 또는 스택트레이스 전체"),
        limit: z.number().int().min(1).max(5).optional().describe("최대 결과 수(기본 3)"),
      },
      annotations: readOnly,
    },
    (input) => run(() => searchKnownIssues(client, webUrl, input)),
  );

  server.registerTool(
    "search_questions",
    {
      title: "Search questions",
      description: "키워드로 Quno 질문을 검색한다. 에러 텍스트가 없거나 search_known_issues에서 찾지 못했을 때 쓴다.",
      inputSchema: {
        query: z.string().min(1).max(200).describe("검색어"),
        limit: z.number().int().min(1).max(10).optional().describe("최대 결과 수(기본 5)"),
      },
      annotations: readOnly,
    },
    (input) => run(() => searchQuestions(client, webUrl, input)),
  );

  server.registerTool(
    "get_question",
    {
      title: "Get question",
      description:
        "질문 하나의 본문·로그·스택 지문과 답변(채택 답변 먼저, 최대 5개)을 가져온다. " +
        "각 답변의 applicability는 그 답변이 유효한 버전 범위다(fromVersion 이상, untilVersion 미만).",
      inputSchema: { id: z.number().int().positive().describe("질문 id") },
      annotations: readOnly,
    },
    (input) => run(() => getQuestion(client, webUrl, input)),
  );

  server.registerTool(
    "get_my_stack",
    {
      title: "Get my stack",
      description: "사용자가 Quno에 저장한 '내 스택'(의존성과 버전)을 가져온다. 답변의 적용 범위와 비교할 때 쓴다.",
      inputSchema: {},
      annotations: readOnly,
    },
    () => run(() => getMyStack(client)),
  );

  if (local) {
    server.registerTool(
      "capture_environment",
      {
        title: "Capture environment",
        description:
          "이 컴퓨터의 환경 스냅샷(OS·JDK·컨테이너·로케일·잠금 파일의 해석된 의존성)을 만들어 보여 준다. 아무 데도 보내지 않는다. " +
          "명령이 실패했으면 failure에 명령(argv)·종료 코드·출력을 넣는다. 그러면 증상(메모리 부족·DB 시간 초과·의존성 해석 실패·테스트 실패·빌드 실패)에 맞는 진단과 " +
          "마지막으로 성공한 뒤 바뀐 것(커밋 수·파일 이름·의존성 버전)을 더하고, 사용자가 quno ask --last-failure로 올릴 수 있게 이 컴퓨터에 기록한다. " +
          "결과는 사용자에게 보여 주고, 질문에 붙일지는 사용자가 정한다.",
        inputSchema: {
          preset: z.enum(["minimal", "standard", "detailed"]).optional().describe("minimal: 환경 식별 정보만, standard: 기본, detailed: 메모리·디스크·cgroup 한도까지(사용자가 원할 때만)"),
          omit: z.array(z.string().max(100)).max(20).optional().describe("뺄 필드(점 구분). 예: tier0.git, tier0.dependencies, tier1.env, delta"),
          failure: z
            .object({
              argv: z.array(z.string().max(1_000)).min(1).max(100).describe("실행한 명령과 인자"),
              exitCode: z.number().int().describe("종료 코드"),
              output: z.string().max(1_000_000).describe("명령의 출력(stdout+stderr). 마지막 200줄만 남긴다"),
            })
            .optional(),
          useLastFailure: z.boolean().optional().describe("새로 만들지 않고 quno run이나 이전 호출이 기록한 마지막 실패를 보여 준다"),
        },
        annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false, openWorldHint: false },
      },
      (input) => run(async () => captureEnvironment(local, input)),
    );
  }

  return server;
}

export async function serveStdio(client: QunoClient, webUrl: string, local?: LocalEnvironment): Promise<void> {
  await createServer(client, webUrl, local).connect(new StdioServerTransport());
}
