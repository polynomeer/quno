import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import type { QuestionDetail } from "../api/question.types";
import { PromotionPanel } from "./PromotionPanel";

const question = (overrides: Partial<QuestionDetail>): QuestionDetail => ({
  id: 7,
  authorId: 1,
  title: "t",
  status: "RESOLVED",
  versionNumber: 1,
  body: "b",
  environment: null,
  logs: null,
  tags: [],
  createdAt: "",
  updatedAt: "",
  score: 0,
  viewCount: 0,
  visibility: "ORG",
  organizationId: 3,
  organizationName: "Acme",
  ...overrides,
});

describe("PromotionPanel", () => {
  it("offers the author of a resolved private question a sanitized public card", () => {
    render(<PromotionPanel question={question({})} isAuthor />);
    expect(screen.getByRole("link", { name: "정제해서 공개하기" })).toHaveAttribute("href", "/questions/7/promote");
  });

  it("stays out of the way for other members, unresolved and public questions", () => {
    const { container, rerender } = render(<PromotionPanel question={question({})} isAuthor={false} />);
    expect(container).toBeEmptyDOMElement();
    rerender(<PromotionPanel question={question({ status: "OPEN" })} isAuthor />);
    expect(container).toBeEmptyDOMElement();
    rerender(<PromotionPanel question={question({ visibility: "PUBLIC", organizationId: null, promotedQuestionId: 9 })} isAuthor />);
    expect(container).toBeEmptyDOMElement();
  });

  it("points members at the card once it exists", () => {
    render(<PromotionPanel question={question({ promotedQuestionId: 42 })} isAuthor={false} />);
    expect(screen.getByRole("link", { name: "공개 카드 보기" })).toHaveAttribute("href", "/questions/42");
    expect(screen.queryByRole("link", { name: "정제해서 공개하기" })).not.toBeInTheDocument();
  });
});
