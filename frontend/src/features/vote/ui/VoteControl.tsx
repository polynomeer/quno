"use client";

import { useSession } from "@/features/auth/hooks/useSession";
import { useMyVotes } from "../hooks/useMyVotes";
import { useCastVote } from "../hooks/useCastVote";
import { useRetractVote } from "../hooks/useRetractVote";
import { cn } from "@/shared/lib/cn";
import type { VoteTargetType } from "../api/vote.types";

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
    return <span className="text-sm font-semibold text-text-secondary">{score}</span>;
  }

  const myValue = myVotes?.find((vote) => vote.targetType === targetType && vote.targetId === targetId)?.value ?? null;
  const isPending = castVote.isPending || retractVote.isPending;

  function handleVote(value: 1 | -1) {
    if (myValue === value) {
      retractVote.mutate();
    } else {
      castVote.mutate(value);
    }
  }

  return (
    <div className="flex flex-col items-center gap-0.5">
      <button
        type="button"
        onClick={() => handleVote(1)}
        disabled={isPending}
        aria-label="Upvote"
        className={cn(
          "leading-none text-text-secondary transition-colors hover:text-brand disabled:pointer-events-none",
          myValue === 1 && "text-brand",
        )}
      >
        ▲
      </button>
      <span className="text-sm font-semibold">{score}</span>
      <button
        type="button"
        onClick={() => handleVote(-1)}
        disabled={isPending}
        aria-label="Downvote"
        className={cn(
          "leading-none text-text-secondary transition-colors hover:text-danger disabled:pointer-events-none",
          myValue === -1 && "text-danger",
        )}
      >
        ▼
      </button>
    </div>
  );
}
