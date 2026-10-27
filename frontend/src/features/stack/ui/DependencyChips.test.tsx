import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { DependencyChips } from "./DependencyChips";
import type { StackDependency } from "../api/stack.types";

describe("DependencyChips", () => {
  it("shows short names with versions and keeps the full coordinate in the title", () => {
    render(
      <DependencyChips
        dependencies={[
          { ecosystem: "MAVEN", name: "io.r2dbc:r2dbc-pool", version: "1.0.2" },
          { ecosystem: "GRADLE_PLUGIN", name: "org.springframework.boot", version: "4.0.8" },
          { ecosystem: "MAVEN", name: "org.springframework.boot:spring-boot-starter-web", version: null },
        ]}
      />,
    );

    expect(screen.getByRole("list", { name: "스택 지문" })).toBeInTheDocument();
    expect(screen.getByTitle("io.r2dbc:r2dbc-pool 1.0.2")).toHaveTextContent("r2dbc-pool1.0.2");
    expect(screen.getByTitle("org.springframework.boot:spring-boot-starter-web")).toHaveTextContent("spring-boot-starter-web");
  });

  it("collapses anything past eight into a count", () => {
    const many: StackDependency[] = Array.from({ length: 11 }, (_, i) => ({ ecosystem: "NPM", name: `pkg${i}`, version: "1.0.0" }));
    render(<DependencyChips dependencies={many} />);

    expect(screen.getByText("+3")).toBeInTheDocument();
  });

  it("renders nothing without dependencies", () => {
    const { container } = render(<DependencyChips dependencies={[]} />);

    expect(container).toBeEmptyDOMElement();
  });
});
