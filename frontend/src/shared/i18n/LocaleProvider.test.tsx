import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { LocaleProvider, useLocale } from "./LocaleProvider";

const STORAGE_KEY = "quno:locale";

function Probe() {
  const { locale, setLocale, t } = useLocale();
  return (
    <div>
      <p data-testid="locale">{locale}</p>
      <p data-testid="text">{t.login.title}</p>
      <button onClick={() => setLocale("en")}>switch to en</button>
      <button onClick={() => setLocale("ko")}>switch to ko</button>
    </div>
  );
}

describe("LocaleProvider", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it("defaults to ko when nothing is stored", () => {
    render(
      <LocaleProvider>
        <Probe />
      </LocaleProvider>,
    );

    expect(screen.getByTestId("locale")).toHaveTextContent("ko");
    expect(screen.getByTestId("text")).toHaveTextContent("로그인");
  });

  it("reads a previously stored locale on mount", () => {
    localStorage.setItem(STORAGE_KEY, "en");

    render(
      <LocaleProvider>
        <Probe />
      </LocaleProvider>,
    );

    expect(screen.getByTestId("locale")).toHaveTextContent("en");
    expect(screen.getByTestId("text")).toHaveTextContent("Log in");
  });

  it("falls back to ko when the stored value is not a supported locale", () => {
    localStorage.setItem(STORAGE_KEY, "fr");

    render(
      <LocaleProvider>
        <Probe />
      </LocaleProvider>,
    );

    expect(screen.getByTestId("locale")).toHaveTextContent("ko");
  });

  it("switches locale and persists the choice to localStorage", async () => {
    render(
      <LocaleProvider>
        <Probe />
      </LocaleProvider>,
    );

    await userEvent.click(screen.getByRole("button", { name: "switch to en" }));

    expect(screen.getByTestId("locale")).toHaveTextContent("en");
    expect(screen.getByTestId("text")).toHaveTextContent("Log in");
    expect(localStorage.getItem(STORAGE_KEY)).toBe("en");
  });

  it("throws when useLocale is called outside a LocaleProvider", () => {
    // 의도적인 콘솔 에러 출력 — React가 렌더 에러를 로그로 남기는 것까지 억제한다.
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

    expect(() => render(<Probe />)).toThrow("useLocale must be used within LocaleProvider");

    consoleError.mockRestore();
  });
});
