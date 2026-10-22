"use client";

import { useSession } from "@/features/auth/hooks/useSession";
import { useMyVotes } from "../hooks/useMyVotes";
import { useCastVote } from "../hooks/useCastVote";
import { useRetractVote } from "../hooks/useRetractVote";
import { cn } from "@/shared/lib/cn";
import type { VoteTargetType } from "../api/vote.types";

const voteButtonClass =
  "grid size-11 place-items-center rounded-full transition-colors disabled:pointer-events-none";

function Chevron({ direction }: { direction: "up" | "down" }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      width="18"
      height="18"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d={direction === "up" ? "m6 15 6-6 6 6" : "m6 9 6 6 6-6"} />
    </svg>
  );
}

/** Backend blocks self-voting (`SelfVoteException`, 403) — show the score plainly instead of
 * interactive buttons when the viewer is the author (design.md #9 Action Rail, scoped down).
 * Anonymous viewers (Phase 29, ADR-0041 — question/answer reading is public) get the same
 * read-only treatment, since voting itself still requires login. */
export function VoteControl({
  targetType,
  targetId,
  questionId,
  score,
  authorId,
}: {
  targetType: VoteTargetType;
  targetId: number;
  questionId: number;
  score: number;
  authorId: number;
}) {
  const { data: me } = useSession();
  const { data: myVotes } = useMyVotes(Boolean(me));
  const castVote = useCastVote(targetType, targetId, questionId);
  const retractVote = useRetractVote(targetType, targetId, questionId);

  if (!me || me.id === authorId) {
    return (
      <span className="w-11 text-center font-mono text-lg font-semibold text-text-primary">
        {score}
      </span>
    );
  }

  const myValue =
    myVotes?.find(
      (vote) => vote.targetType === targetType && vote.targetId === targetId,
    )?.value ?? null;
  const isPending = castVote.isPending || retractVote.isPending;

  function handleVote(value: 1 | -1) {
    if (myValue === value) {
      retractVote.mutate();
    } else {
      castVote.mutate(value);
    }
  }

  return (
    // Glass rail from web design v2 (ADR-0071) — floating chrome, not content.
    <div className="glass flex flex-col items-center gap-0.5 rounded-[28px] p-1.5">
      <button
        type="button"
        onClick={() => handleVote(1)}
        disabled={isPending}
        aria-label="Upvote"
        aria-pressed={myValue === 1}
        className={cn(
          voteButtonClass,
          // clsx doesn't dedupe Tailwind classes, so each state sets its own color exactly once.
          myValue === 1
            ? "bg-brand text-brand-foreground"
            : "text-text-body hover:bg-brand-subtle hover:text-brand-on-subtle",
        )}
      >
        <Chevron direction="up" />
      </button>
      <span className="font-mono text-lg font-semibold text-text-primary">
        {score}
      </span>
      <button
        type="button"
        onClick={() => handleVote(-1)}
        disabled={isPending}
        aria-label="Downvote"
        aria-pressed={myValue === -1}
        className={cn(
          voteButtonClass,
          myValue === -1 ? "bg-danger-subtle text-danger" : "text-text-body hover:bg-danger-subtle hover:text-danger",
        )}
      >
        <Chevron direction="down" />
      </button>
    </div>
  );
}
