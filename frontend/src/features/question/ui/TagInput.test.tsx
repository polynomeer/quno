import { useState } from "react";
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { TagInput } from "./TagInput";

function ControlledTagInput({ initial = [] as string[] }) {
  const [value, setValue] = useState<string[]>(initial);
  return <TagInput value={value} onChange={setValue} />;
}

describe("TagInput", () => {
  it("adds a tag when Enter is pressed", async () => {
    render(<ControlledTagInput />);

    await userEvent.type(screen.getByPlaceholderText("태그 입력 후 Enter"), "kotlin{Enter}");

    expect(screen.getByText("kotlin")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("태그 입력 후 Enter")).toHaveValue("");
  });

  it("adds a tag when a trailing comma is typed", async () => {
    render(<ControlledTagInput />);

    await userEvent.type(screen.getByPlaceholderText("태그 입력 후 Enter"), "kotlin,");

    expect(screen.getByText("kotlin")).toBeInTheDocument();
  });

  it("does not add a duplicate of an existing tag", async () => {
    render(<ControlledTagInput initial={["kotlin"]} />);

    await userEvent.type(screen.getByPlaceholderText("태그 입력 후 Enter"), "kotlin{Enter}");

    expect(screen.getAllByText("kotlin")).toHaveLength(1);
  });

  it("does not add a blank tag", async () => {
    render(<ControlledTagInput />);

    await userEvent.type(screen.getByPlaceholderText("태그 입력 후 Enter"), "   {Enter}");

    expect(screen.queryByLabelText(/Remove /)).not.toBeInTheDocument();
  });

  it("removes the last tag on Backspace when the draft is empty", async () => {
    render(<ControlledTagInput initial={["kotlin", "spring-boot"]} />);

    await userEvent.type(screen.getByPlaceholderText("태그 입력 후 Enter"), "{Backspace}");

    expect(screen.queryByText("spring-boot")).not.toBeInTheDocument();
    expect(screen.getByText("kotlin")).toBeInTheDocument();
  });

  it("removes a specific tag via its × button", async () => {
    render(<ControlledTagInput initial={["kotlin", "spring-boot"]} />);

    await userEvent.click(screen.getByLabelText("Remove kotlin"));

    expect(screen.queryByText("kotlin")).not.toBeInTheDocument();
    expect(screen.getByText("spring-boot")).toBeInTheDocument();
  });

  it("disables the input and shows the limit message once 5 tags are added", () => {
    render(<ControlledTagInput initial={["a", "b", "c", "d", "e"]} />);

    const input = screen.getByPlaceholderText("최대 5개까지 추가할 수 있습니다");
    expect(input).toBeDisabled();
  });

  it("adds the drafted text on blur", async () => {
    render(<ControlledTagInput />);
    const input = screen.getByPlaceholderText("태그 입력 후 Enter");
    await userEvent.type(input, "kotlin");

    await userEvent.tab();

    expect(screen.getByText("kotlin")).toBeInTheDocument();
  });
});
