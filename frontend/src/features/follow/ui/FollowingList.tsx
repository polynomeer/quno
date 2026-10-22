import Link from "next/link";
import type { Followee } from "../api/follow.types";
import { EmptyState } from "@/shared/ui/EmptyState";

export function FollowingList({ users, emptyMessage }: { users: Followee[]; emptyMessage: string }) {
  if (users.length === 0) {
    return <EmptyState message={emptyMessage} />;
  }

  return (
    <ul className="divide-y divide-border overflow-hidden rounded-[20px] border border-border bg-surface">
      {users.map((user) => (
        <li key={user.userId} className="px-4 py-3 text-sm transition-colors hover:bg-canvas/60 sm:px-5">
          <Link href={`/users/${user.userId}`} className="font-medium text-text-primary hover:text-brand">
            {user.nickname}
          </Link>
        </li>
      ))}
    </ul>
  );
}
