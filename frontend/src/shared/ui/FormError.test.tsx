import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { FormError } from "./FormError";
import { ApiError } from "@/shared/api/api-error";

describe("FormError", () => {
  it("renders nothing when there is no error", () => {
    const { container } = render(<FormError error={null} fallback="실패했습니다." />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows the ApiError's own message", () => {
    render(<FormError error={new ApiError(400, "BAD_REQUEST", "이미 사용 중인 이메일입니다.")} fallback="실패했습니다." />);
    expect(screen.getByRole("alert")).toHaveTextContent("이미 사용 중인 이메일입니다.");
  });

  it("shows a plain string error verbatim", () => {
    render(<FormError error="결제창을 여는 데 실패했습니다." fallback="실패했습니다." />);
    expect(screen.getByRole("alert")).toHaveTextContent("결제창을 여는 데 실패했습니다.");
  });

  it("falls back to the given message for a generic Error", () => {
    render(<FormError error={new Error("network down")} fallback="실패했습니다." />);
    expect(screen.getByRole("alert")).toHaveTextContent("실패했습니다.");
  });

  it("defaults to text-sm and switches to text-xs when size is xs", () => {
    const { rerender } = render(<FormError error="x" fallback="실패했습니다." />);
    expect(screen.getByRole("alert")).toHaveClass("text-sm");

    rerender(<FormError error="x" fallback="실패했습니다." size="xs" />);
    expect(screen.getByRole("alert")).toHaveClass("text-xs");
  });

  it("merges an extra className without dropping the size class", () => {
    render(<FormError error="x" fallback="실패했습니다." className="mt-2" />);
    const el = screen.getByRole("alert");
    expect(el).toHaveClass("text-sm");
    expect(el).toHaveClass("mt-2");
  });
});
