import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StackFingerprintInput } from "./StackFingerprintInput";
import { stackApi } from "../api/stack.api";
import { LocaleProvider } from "@/shared/i18n/LocaleProvider";

vi.mock("../api/stack.api", () => ({ stackApi: { parse: vi.fn() } }));

const PARSED = {
  format: "GRADLE" as const,
  dependencies: [
    { ecosystem: "MAVEN" as const, name: "io.r2dbc:r2dbc-pool", version: "1.0.2" },
    { ecosystem: "MAVEN" as const, name: "com.acme:billing-common", version: "0.14.0" },
  ],
};

function renderInput(onChange = vi.fn()) {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <LocaleProvider>
        <StackFingerprintInput onChange={onChange} />
      </LocaleProvider>
    </QueryClientProvider>,
  );
  return onChange;
}

describe("StackFingerprintInput", () => {
  beforeEach(() => {
    vi.mocked(stackApi.parse).mockReset();
  });

  it("parses the pasted file and reports every entry until the author unticks one", async () => {
    vi.mocked(stackApi.parse).mockResolvedValue(PARSED);
    const onChange = renderInput();

    await userEvent.selectOptions(screen.getByRole("combobox", { name: "파일 종류" }), "build.gradle.kts");
    await userEvent.type(screen.getByLabelText("의존성 파일 내용"), "implementation io.r2dbc");
    await userEvent.click(screen.getByRole("button", { name: "의존성 읽기" }));

    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith(PARSED.dependencies));
    expect(stackApi.parse).toHaveBeenCalledWith("implementation io.r2dbc", "build.gradle.kts");
    expect(screen.getByText("2/2개 붙임")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("checkbox", { name: /com\.acme:billing-common/ }));

    expect(onChange).toHaveBeenLastCalledWith([PARSED.dependencies[0]]);
    expect(screen.getByText("1/2개 붙임")).toBeInTheDocument();
  });

  it("says so when nothing could be read", async () => {
    vi.mocked(stackApi.parse).mockResolvedValue({ format: "GRADLE", dependencies: [] });
    renderInput();

    await userEvent.type(screen.getByLabelText("의존성 파일 내용"), "nothing here");
    await userEvent.click(screen.getByRole("button", { name: "의존성 읽기" }));

    expect(await screen.findByText("읽을 수 있는 의존성이 없어요.")).toBeInTheDocument();
  });

  it("keeps the read button disabled until something is pasted", () => {
    renderInput();

    expect(screen.getByRole("button", { name: "의존성 읽기" })).toBeDisabled();
  });
});
