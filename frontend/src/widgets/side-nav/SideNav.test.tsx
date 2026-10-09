import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { LocaleProvider } from "@/shared/i18n/LocaleProvider";
import { SideNav } from "./SideNav";

const pathname = vi.fn(() => "/");

vi.mock("next/navigation", () => ({
  usePathname: () => pathname(),
}));

function renderNav(wardChangeCount = 0) {
  return render(
    <LocaleProvider>
      <SideNav wardChangeCount={wardChangeCount} />
    </LocaleProvider>,
  );
}

describe("SideNav", () => {
  beforeEach(() => {
    pathname.mockReturnValue("/");
  });

  it("links every main and activity route, with Quno Flow pointing at the home section", () => {
    renderNav();

    expect(screen.getByRole("link", { name: "홈" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "질문" })).toHaveAttribute("href", "/questions");
    expect(screen.getByRole("link", { name: "태그" })).toHaveAttribute("href", "/tags");
    expect(screen.getByRole("link", { name: "Quno Flow" })).toHaveAttribute("href", "/#quno-flow");
    expect(screen.getByRole("link", { name: "조직" })).toHaveAttribute("href", "/organizations");
    expect(screen.getByRole("link", { name: "Watching" })).toHaveAttribute("href", "/watching");
    expect(screen.getByRole("link", { name: "저장됨" })).toHaveAttribute("href", "/saved");
    expect(screen.getByRole("link", { name: "Direct Ask" })).toHaveAttribute("href", "/direct-asks");
  });

  it("marks only the current route with aria-current", () => {
    pathname.mockReturnValue("/tags/kotlin");
    renderNav();

    expect(screen.getByRole("link", { name: "태그" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "홈" })).not.toHaveAttribute("aria-current");
  });

  it("shows the unread Ward change count next to Watching only when there is one", () => {
    const { unmount } = renderNav(3);
    expect(screen.getByRole("link", { name: "Watching 3 변화" })).toBeInTheDocument();
    unmount();

    renderNav(0);
    expect(screen.getByRole("link", { name: "Watching" })).toBeInTheDocument();
  });
});
