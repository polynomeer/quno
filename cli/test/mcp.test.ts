import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import { describe, expect, it } from "vitest";
import { MAX_FIELD_CHARS } from "../src/limits.js";
import { createServer } from "../src/mcp/server.js";
import { QUESTION_SUMMARY, fakeClient } from "./fake-api.js";

const answer = (id: number, extra: object = {}) => ({
  id,
  body: `answer ${id}`,
  isAccepted: false,
  isStale: false,
  score: 0,
  targetVersionNumber: 1,
  createdAt: "",
  applicability: [],
  ...extra,
});

async function connect(routes: Parameters<typeof fakeClient>[0]) {
  const { client } = fakeClient(routes);
  const server = createServer(client, "http://web.test");
  const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
  await server.connect(serverTransport);
  const mcp = new Client({ name: "test", version: "0" });
  await mcp.connect(clientTransport);
  return mcp;
}

describe("quno mcp", () => {
  it("offers four read-only tools", async () => {
    const mcp = await connect({});
    const { tools } = await mcp.listTools();

    expect(tools.map((tool) => tool.name).sort()).toEqual(["get_my_stack", "get_question", "search_known_issues", "search_questions"]);
    expect(tools.every((tool) => tool.annotations?.readOnlyHint === true)).toBe(true);
  });

  it("search_known_issues returns matches with links and a next step", async () => {
    const mcp = await connect({
      "POST /api/v1/error-signatures/preview": () => ({
        json: { signatures: [{ exceptionClass: "java.sql.SQLException", signatureHash: "abc", messageKey: "k", normalizedMessage: "", frames: [] }], questions: [QUESTION_SUMMARY] },
      }),
    });
    const result = await mcp.callTool({ name: "search_known_issues", arguments: { errorText: "java.sql.SQLException: boom" } });

    expect(result.structuredContent).toMatchObject({
      signatures: [{ exceptionClass: "java.sql.SQLException", signatureHash: "abc" }],
      matches: [{ id: 11, url: "http://web.test/questions/11", hasAcceptedAnswer: true }],
    });
  });

  it("get_question puts the accepted answer first, clips long bodies and caps the answers", async () => {
    const mcp = await connect({
      "GET /api/v1/questions/11": () => ({
        json: {
          ...QUESTION_SUMMARY,
          versionNumber: 1,
          body: "b".repeat(MAX_FIELD_CHARS + 10),
          environment: null,
          logs: null,
          dependencies: [],
          environmentSnapshot: {
            source: "COLLECTOR",
            collector: "quno-cli 0.2.0",
            fingerprint: "0123456789abcdef",
            serverRedactions: 0,
            document: { tier0: { os: { name: "macOS", arch: "arm64" }, runtimes: [{ name: "java", vendor: "Amazon Corretto", version: "21.0.10" }], jvmArgs: ["-Xmx2g"] } },
          },
        },
      }),
      "GET /api/v1/questions/11/answers": () => ({
        json: [
          answer(1, { score: 9 }),
          answer(2, { isAccepted: true, applicability: [{ ecosystem: "GRADLE_PLUGIN", name: "org.springframework.boot", fromVersion: "3.2.0", untilVersion: "4.0.0" }] }),
          ...[3, 4, 5, 6, 7].map((id) => answer(id)),
        ],
      }),
    });
    const result = await mcp.callTool({ name: "get_question", arguments: { id: 11 } });
    const value = result.structuredContent as { body: string; bodyOrLogsTruncated: boolean; answers: { id: number; applicability: unknown[] }[]; answerCount: number; moreAnswersAt: string };

    expect(value.answers.map((a) => a.id)).toEqual([2, 1, 3, 4, 5]);
    expect(value.answers[0]?.applicability).toHaveLength(1);
    expect(value.body.length).toBe(MAX_FIELD_CHARS + 1);
    expect(value.bodyOrLogsTruncated).toBe(true);
    expect(value).toMatchObject({ answerCount: 7, moreAnswersAt: "http://web.test/questions/11" });
    expect(value).toMatchObject({
      environmentSnapshot: { fingerprint: "0123456789abcdef", source: "COLLECTOR", runtimes: [{ name: "java", vendor: "Amazon Corretto", version: "21.0.10" }] },
    });
    expect(JSON.stringify(value), "only the summary an agent needs").not.toContain("-Xmx2g");
  });

  it("reports API failures as tool errors instead of crashing", async () => {
    const mcp = await connect({ "GET /api/v1/me/stack": () => ({ status: 401, json: { code: "UNAUTHORIZED", message: "토큰이 유효하지 않거나 만료·폐기되었습니다." } }) });
    const result = await mcp.callTool({ name: "get_my_stack", arguments: {} });

    expect(result.isError).toBe(true);
    expect(JSON.stringify(result.content)).toContain("401 UNAUTHORIZED");
  });
});
