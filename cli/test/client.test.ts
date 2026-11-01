import { describe, expect, it } from "vitest";
import { MAX_SIGNATURE_INPUT, QunoApiError } from "../src/client.js";
import { fakeClient } from "./fake-api.js";

describe("QunoClient", () => {
  it("sends the token as a bearer header and encodes the query", async () => {
    const { client, calls } = fakeClient({ "GET /api/v1/search": () => ({ json: [] }) });
    await client.search("pool exhausted & more", 3);

    expect(calls[0]).toMatchObject({ path: "/api/v1/search?q=pool%20exhausted%20%26%20more&limit=3", authorization: "Bearer quno_pat_test" });
  });

  it("omits the header without a token", async () => {
    const { client, calls } = fakeClient({ "GET /api/v1/search": () => ({ json: [] }) }, null);
    await client.search("x");
    expect(calls[0]?.authorization).toBeNull();
  });

  it("turns error bodies into QunoApiError with the server's code and message", async () => {
    const { client } = fakeClient({ "GET /api/v1/me": () => ({ status: 403, json: { code: "FORBIDDEN", message: "권한 없음" } }) });
    await expect(client.me()).rejects.toMatchObject(new QunoApiError(403, "FORBIDDEN", "권한 없음"));
  });

  it("keeps the tail of an over-long trace, where the root cause is", async () => {
    const { client, calls } = fakeClient({ "POST /api/v1/error-signatures/preview": () => ({ json: { signatures: [], questions: [] } }) });
    await client.previewErrorSignatures("x".repeat(30_000) + "Caused by: java.io.IOException");

    const sent = (calls[0]?.body as { text: string }).text;
    expect(sent).toHaveLength(MAX_SIGNATURE_INPUT);
    expect(sent.endsWith("Caused by: java.io.IOException")).toBe(true);
  });

  it("reports an unreachable server as status 0", async () => {
    const client = new (await import("../src/client.js")).QunoClient("http://api.test", null, (async () => {
      throw new Error("ECONNREFUSED");
    }) as typeof fetch);
    await expect(client.me()).rejects.toMatchObject({ status: 0, code: "UNREACHABLE" });
  });
});
