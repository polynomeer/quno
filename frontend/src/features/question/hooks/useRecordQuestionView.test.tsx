import type { ReactNode } from "react";
import { StrictMode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useRecordQuestionView } from "./useRecordQuestionView";
import { questionApi } from "../api/question.api";

vi.mock("../api/question.api", () => ({
  questionApi: { recordView: vi.fn() },
}));

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient();
  return (
    <StrictMode>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </StrictMode>
  );
}

describe("useRecordQuestionView", () => {
  beforeEach(() => {
    vi.mocked(questionApi.recordView).mockReset().mockResolvedValue(undefined);
  });

  it("records exactly one view per question, even under StrictMode's double effect", async () => {
    renderHook(() => useRecordQuestionView(7), { wrapper });

    await waitFor(() => expect(questionApi.recordView).toHaveBeenCalledWith(7));
    expect(questionApi.recordView).toHaveBeenCalledTimes(1);
  });

  it("swallows a failure so reading the page is never affected", async () => {
    vi.mocked(questionApi.recordView).mockRejectedValue(new Error("network"));

    expect(() => renderHook(() => useRecordQuestionView(8), { wrapper })).not.toThrow();
    await waitFor(() => expect(questionApi.recordView).toHaveBeenCalledWith(8));
  });
});
