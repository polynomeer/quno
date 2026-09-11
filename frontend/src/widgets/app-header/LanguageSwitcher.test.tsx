import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { LanguageSwitcher } from "./LanguageSwitcher";
import { LocaleProvider } from "@/shared/i18n/LocaleProvider";

function renderSwitcher() {
  return render(
    <LocaleProvider>
      <LanguageSwitcher />
    </LocaleProvider>,
  );
}

describe("LanguageSwitcher", () => {
  it("marks 한국어 as pressed by default", () => {
    renderSwitcher();

    expect(screen.getByRole("button", { name: "한국어" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "English" })).toHaveAttribute("aria-pressed", "false");
  });

  it("switches the pressed state when English is clicked", async () => {
    renderSwitcher();

    await userEvent.click(screen.getByRole("button", { name: "English" }));

    expect(screen.getByRole("button", { name: "English" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "한국어" })).toHaveAttribute("aria-pressed", "false");
  });
});
