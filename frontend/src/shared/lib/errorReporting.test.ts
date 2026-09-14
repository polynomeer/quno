import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@sentry/browser", () => ({
  init: vi.fn(),
  captureException: vi.fn(),
}));

const ORIGINAL_DSN = process.env.NEXT_PUBLIC_SENTRY_DSN;

describe("reportError", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
  });

  afterEach(() => {
    process.env.NEXT_PUBLIC_SENTRY_DSN = ORIGINAL_DSN;
  });

  it("always logs the error to the console", async () => {
    delete process.env.NEXT_PUBLIC_SENTRY_DSN;
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const { reportError } = await import("./errorReporting");
    const error = new Error("boom");

    await reportError(error);

    expect(consoleError).toHaveBeenCalledWith(error);
    consoleError.mockRestore();
  });

  it("does not initialize or call Sentry when no DSN is configured", async () => {
    delete process.env.NEXT_PUBLIC_SENTRY_DSN;
    vi.spyOn(console, "error").mockImplementation(() => {});
    const { reportError } = await import("./errorReporting");
    const Sentry = await import("@sentry/browser");

    await reportError(new Error("boom"));

    expect(Sentry.init).not.toHaveBeenCalled();
    expect(Sentry.captureException).not.toHaveBeenCalled();
  });

  it("initializes Sentry with the DSN and reports the error when a DSN is configured", async () => {
    process.env.NEXT_PUBLIC_SENTRY_DSN = "https://example.ingest.sentry.io/1";
    vi.spyOn(console, "error").mockImplementation(() => {});
    const { reportError } = await import("./errorReporting");
    const Sentry = await import("@sentry/browser");
    const error = new Error("boom");

    await reportError(error, { userId: 1 });

    expect(Sentry.init).toHaveBeenCalledWith(
      expect.objectContaining({ dsn: "https://example.ingest.sentry.io/1" }),
    );
    expect(Sentry.captureException).toHaveBeenCalledWith(error, { extra: { userId: 1 } });
  });

  it("does not re-initialize Sentry on a second call", async () => {
    process.env.NEXT_PUBLIC_SENTRY_DSN = "https://example.ingest.sentry.io/1";
    vi.spyOn(console, "error").mockImplementation(() => {});
    const { reportError } = await import("./errorReporting");
    const Sentry = await import("@sentry/browser");

    await reportError(new Error("first"));
    await reportError(new Error("second"));

    expect(Sentry.init).toHaveBeenCalledTimes(1);
    expect(Sentry.captureException).toHaveBeenCalledTimes(2);
  });
});
