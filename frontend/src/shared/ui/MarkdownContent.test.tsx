import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { MarkdownContent } from "./MarkdownContent";

describe("MarkdownContent", () => {
  it("renders emphasis and headings as real elements", () => {
    render(<MarkdownContent>{"# Title\n\nSome **bold** text."}</MarkdownContent>);

    expect(screen.getByRole("heading", { name: "Title" })).toBeInTheDocument();
    expect(screen.getByText("bold").tagName).toBe("STRONG");
  });

  it("renders GFM tables via remark-gfm", () => {
    const markdown = ["| a | b |", "| - | - |", "| 1 | 2 |"].join("\n");
    render(<MarkdownContent>{markdown}</MarkdownContent>);

    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "1" })).toBeInTheDocument();
  });

  it("does not execute embedded raw HTML/scripts (renders them as inert text instead)", () => {
    render(<MarkdownContent>{'<img src=x onerror="window.__pwned = true" />'}</MarkdownContent>);

    expect((window as typeof window & { __pwned?: boolean }).__pwned).toBeUndefined();
    expect(document.querySelector("img")).not.toBeInTheDocument();
  });

  it("merges an extra className onto the wrapper", () => {
    const { container } = render(<MarkdownContent className="prose">hello</MarkdownContent>);
    expect(container.firstChild).toHaveClass("markdown-body", "prose");
  });
});
