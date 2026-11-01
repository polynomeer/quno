import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { QunoApiError, type QunoClient } from "../client.js";
import { getMyStack, getQuestion, searchKnownIssues, searchQuestions, type ToolOutput } from "./tools.js";

const VERSION = "0.1.0";
const readOnly = { readOnlyHint: true, openWorldHint: true } as const;

async function run(produce: () => Promise<ToolOutput>) {
  try {
    const output = await produce();
    return { content: [{ type: "text" as const, text: output.json }], structuredContent: output.value };
  } catch (error) {
    const message = error instanceof QunoApiError ? `Quno API ${error.status} ${error.code}: ${error.message}` : String(error);
    return { content: [{ type: "text" as const, text: message }], isError: true };
  }
}

/** Read-only tools over the Quno API (Phase 65.3, ADR-0080). A READ-scoped token is enough. */
export function createServer(client: QunoClient, webUrl: string): McpServer {
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

  return server;
}

export async function serveStdio(client: QunoClient, webUrl: string): Promise<void> {
  await createServer(client, webUrl).connect(new StdioServerTransport());
}
