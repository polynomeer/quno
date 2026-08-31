"use client";

import { useSession } from "@/features/auth/hooks/useSession";
import { useMyFollowing } from "../hooks/useMyFollowing";
import { useToggleFollow } from "../hooks/useToggleFollow";
import { Button } from "@/shared/ui/Button";

/** Backend blocks self-follow (`SelfFollowException`, 403) — the button hides itself on your
 * own profile rather than relying on every caller to remember the check (mirrors VoteControl). */
export function FollowUserButton({ userId }: { userId: number }) {
  const { data: me } = useSession();
  const { data: following, isLoading } = useMyFollowing(Boolean(me));
  const toggleFollow = useToggleFollow(userId);

  if (me && me.id === userId) {
    return null;
  }

  const isFollowing = Boolean(following?.some((f) => f.userId === userId));

  return (
    <Button
      variant={isFollowing ? "secondary" : "primary"}
      onClick={() => toggleFollow.mutate(isFollowing)}
      disabled={isLoading || toggleFollow.isPending}
    >
      {isFollowing ? "Following" : "Follow"}
    </Button>
  );
}
