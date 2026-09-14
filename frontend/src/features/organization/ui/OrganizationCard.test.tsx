import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { OrganizationCard } from "./OrganizationCard";
import type { Organization } from "@/entities/organization/model/organization.types";

const ORG: Organization = {
  id: 7,
  name: "Quno",
  description: null,
  createdBy: 1,
  memberCount: 3,
  emailDomain: null,
  verified: false,
  createdAt: "",
};

describe("OrganizationCard", () => {
  it("links the name to the organization's detail page", () => {
    render(
      <ul>
        <OrganizationCard organization={ORG} />
      </ul>,
    );
    expect(screen.getByRole("link", { name: "Quno" })).toHaveAttribute("href", "/organizations/7");
  });

  it("shows the member count", () => {
    render(
      <ul>
        <OrganizationCard organization={ORG} />
      </ul>,
    );
    expect(screen.getByText("멤버 3명")).toBeInTheDocument();
  });

  it("shows the description when present", () => {
    render(
      <ul>
        <OrganizationCard organization={{ ...ORG, description: "개발자 Q&A 조직" }} />
      </ul>,
    );
    expect(screen.getByText("개발자 Q&A 조직")).toBeInTheDocument();
  });

  it("does not show a Verified badge for a non-verified organization", () => {
    render(
      <ul>
        <OrganizationCard organization={ORG} />
      </ul>,
    );
    expect(screen.queryByText(/Verified/)).not.toBeInTheDocument();
  });

  it("shows the Verified badge with its email domain for a Verified organization", () => {
    render(
      <ul>
        <OrganizationCard organization={{ ...ORG, verified: true, emailDomain: "acme.com" }} />
      </ul>,
    );
    expect(screen.getByText("Verified · acme.com")).toBeInTheDocument();
  });
});
