import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { LocaleProvider } from "@/shared/i18n/LocaleProvider";
import type { Organization } from "@/entities/organization/model/organization.types";
import { VisibilityPicker } from "./VisibilityPicker";

const org = (id: number, name: string, verified: boolean): Organization => ({
  id,
  name,
  description: null,
  createdBy: 1,
  memberCount: 3,
  emailDomain: verified ? `${name}.com` : null,
  verified,
  createdAt: "",
});

function renderPicker(organizations: Organization[], value: number | null = null) {
  const onChange = vi.fn();
  render(
    <LocaleProvider>
      <VisibilityPicker organizations={organizations} value={value} onChange={onChange} />
    </LocaleProvider>,
  );
  return onChange;
}

describe("VisibilityPicker", () => {
  it("offers public plus verified organizations only", async () => {
    const onChange = renderPicker([org(1, "acme", true), org(2, "study-group", false)]);

    expect(screen.getByRole("radio", { name: /전체 공개/ })).toBeChecked();
    expect(screen.queryByRole("radio", { name: /study-group/ })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("radio", { name: /acme 멤버만/ }));
    expect(onChange).toHaveBeenCalledWith(1);
  });

  it("points to organization verification when there is no verified organization", () => {
    renderPicker([org(2, "study-group", false)]);

    expect(screen.getAllByRole("radio")).toHaveLength(1);
    expect(screen.getByRole("link", { name: "조직 인증하기" })).toHaveAttribute("href", "/organizations");
  });
});
