import Link from "next/link";
import type { Followee } from "../api/follow.types";

export function FollowingList({ users, emptyMessage }: { users: Followee[]; emptyMessage: string }) {
  if (users.length === 0) {
    return <p className="text-sm text-text-secondary">{emptyMessage}</p>;
  }

  return (
    <ul className="flex flex-wrap gap-2">
      {users.map((user) => (
        <li key={user.userId}>
          <Link
            href={`/users/${user.userId}`}
            className="inline-flex items-center rounded-full border border-border px-3 py-1 text-sm hover:bg-surface-subtle"
          >
            {user.nickname}
          </Link>
        </li>
      ))}
    </ul>
  );
}
