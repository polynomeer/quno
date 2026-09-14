import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { RelatedTagList } from "./RelatedTagList";
import type { Tag } from "@/entities/tag/model/tag.types";

function tag(id: number, name: string): Tag {
  return { id, name, slug: name, description: null, docsUrl: null };
}

describe("RelatedTagList", () => {
  it("shows the empty message when there are no related tags", () => {
    render(<RelatedTagList tags={[]} />);
    expect(screen.getByText("관련 태그가 없습니다.")).toBeInTheDocument();
  });

  it("renders a tag chip for every related tag, linking to its detail page", () => {
    render(<RelatedTagList tags={[tag(1, "kotlin"), tag(2, "spring-boot")]} />);

    expect(screen.getByRole("link", { name: "kotlin" })).toHaveAttribute("href", "/tags/kotlin");
    expect(screen.getByRole("link", { name: "spring-boot" })).toHaveAttribute("href", "/tags/spring-boot");
  });
});
