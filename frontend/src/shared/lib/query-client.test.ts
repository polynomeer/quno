import { describe, expect, it } from "vitest";
import { QueryClient } from "@tanstack/react-query";
import { createQueryClient } from "./query-client";

describe("createQueryClient", () => {
  it("returns a QueryClient instance", () => {
    expect(createQueryClient()).toBeInstanceOf(QueryClient);
  });

  it("configures a 30s staleTime and 1 retry for queries by default", () => {
    const client = createQueryClient();
    const defaults = client.getDefaultOptions();

    expect(defaults.queries?.staleTime).toBe(30_000);
    expect(defaults.queries?.retry).toBe(1);
  });

  it("returns a fresh instance on every call", () => {
    expect(createQueryClient()).not.toBe(createQueryClient());
  });
});
