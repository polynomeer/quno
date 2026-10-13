import Link from "next/link";
import type { Followee } from "../api/follow.types";

export function FollowingList({ users, emptyMessage }: { users: Followee[]; emptyMessage: string }) {
  if (users.length === 0) {
    return <p className="text-sm text-text-secondary">{emptyMessage}</p>;
  }

  return (
    <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
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
