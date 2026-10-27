import { useState } from "react";
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApplicabilityRangeEditor } from "./ApplicabilityRangeEditor";
import { ApplicabilityChips } from "./ApplicabilityChips";
import type { ApplicabilityRow } from "../lib/applicability";
import type { StackDependency } from "../api/stack.types";

const BOOT: StackDependency = { ecosystem: "GRADLE_PLUGIN", name: "org.springframework.boot", version: "4.0.8" };

function Harness({ suggestions = [] }: { suggestions?: StackDependency[] }) {
  const [rows, setRows] = useState<ApplicabilityRow[]>([]);
  return (
    <>
      <ApplicabilityRangeEditor rows={rows} onChange={setRows} suggestions={suggestions} idPrefix="t" />
      <output data-testid="rows">{JSON.stringify(rows)}</output>
    </>
  );
}

const rowsOf = () => JSON.parse(screen.getByTestId("rows").textContent ?? "[]") as ApplicabilityRow[];

describe("ApplicabilityRangeEditor", () => {
  it("adds a question dependency as a row starting at its version, then stops offering it", async () => {
    render(<Harness suggestions={[BOOT]} />);

    await userEvent.click(screen.getByRole("button", { name: "+ org.springframework.boot" }));

    expect(rowsOf()).toEqual([{ ecosystem: "GRADLE_PLUGIN", name: "org.springframework.boot", fromVersion: "4.0.8", untilVersion: "" }]);
    expect(screen.queryByRole("button", { name: "+ org.springframework.boot" })).not.toBeInTheDocument();
  });

  it("edits a typed-in package and removes it", async () => {
    render(<Harness />);
    await userEvent.click(screen.getByRole("button", { name: "+ 패키지 직접 추가" }));

    await userEvent.selectOptions(screen.getByLabelText("생태계"), "NPM");
    await userEvent.type(screen.getByLabelText("패키지"), "react");
    await userEvent.type(screen.getByLabelText("미만"), "19.0.0");
    expect(rowsOf()).toEqual([{ ecosystem: "NPM", name: "react", fromVersion: "", untilVersion: "19.0.0" }]);

    await userEvent.click(screen.getByRole("button", { name: "react 범위 삭제" }));
    expect(rowsOf()).toEqual([]);
  });
});

describe("ApplicabilityChips", () => {
  it("shows each range with its short name and nothing when empty", () => {
    const { container, rerender } = render(<ApplicabilityChips ranges={[]} />);
    expect(container).toBeEmptyDOMElement();

    rerender(
      <ApplicabilityChips
        ranges={[{ ecosystem: "MAVEN", name: "io.r2dbc:r2dbc-pool", fromVersion: "1.0.0", untilVersion: "2.0.0" }]}
      />,
    );
    const chip = screen.getByRole("listitem");
    expect(chip).toHaveTextContent("r2dbc-pool");
    expect(chip).toHaveTextContent("1.0.0 이상 · 2.0.0 미만");
    expect(chip).toHaveAttribute("title", "io.r2dbc:r2dbc-pool");
  });
});
