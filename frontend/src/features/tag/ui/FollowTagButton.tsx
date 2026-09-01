"use client";

import { useSession } from "@/features/auth/hooks/useSession";
import { useUserProfile } from "@/entities/user/hooks/useUserProfile";
import { useToggleFollowTag } from "../hooks/useToggleFollowTag";
import { Button } from "@/shared/ui/Button";

/** Follow status isn't on TagResponse (follow is idempotent, backend doesn't need it) — the
 * viewer's own profile (which lists `followedTags`) is the source of truth, same pattern as
 * JoinOrganizationButton (ADR-0021 deferred this until that pattern existed). */
export function FollowTagButton({ tagId }: { tagId: number }) {
  const { data: me } = useSession();
  const { data: myProfile, isLoading } = useUserProfile(me?.id ?? 0, Boolean(me));
  const toggleFollow = useToggleFollowTag(tagId, me?.id);

  if (!me) {
    return null;
  }

  const isFollowing = Boolean(myProfile?.followedTags.some((t) => t.id === tagId));

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
