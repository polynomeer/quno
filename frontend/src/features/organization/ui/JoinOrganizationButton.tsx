"use client";

import { useSession } from "@/features/auth/hooks/useSession";
import { useUserProfile } from "@/entities/user/hooks/useUserProfile";
import { useToggleJoinOrganization } from "../hooks/useToggleJoinOrganization";
import { Button } from "@/shared/ui/Button";
import type { Organization } from "@/entities/organization/model/organization.types";

/** Membership isn't in OrganizationResponse (join is idempotent, backend doesn't need it) — the
 * viewer's own profile (which does list `organizations`) is the source of truth here, same
 * pattern as FollowUserButton reading useMyFollowing instead of a flag on the target. */
export function JoinOrganizationButton({ organization }: { organization: Organization }) {
  const { data: me } = useSession();
  const { data: myProfile, isLoading } = useUserProfile(me?.id ?? 0, Boolean(me));
  const toggleJoin = useToggleJoinOrganization(organization.id, me?.id);

  if (!me) {
    return null;
  }

  const isMember = Boolean(myProfile?.organizations.some((o) => o.id === organization.id));

  if (organization.verified && !isMember) {
    return <p className="text-xs text-text-secondary">업무/학교 이메일 인증으로만 가입할 수 있습니다.</p>;
  }

  return (
    <Button
      variant={isMember ? "secondary" : "primary"}
      onClick={() => toggleJoin.mutate(isMember)}
      disabled={isLoading || toggleJoin.isPending}
    >
      {isMember ? "가입됨 — 탈퇴" : "가입하기"}
    </Button>
  );
}
