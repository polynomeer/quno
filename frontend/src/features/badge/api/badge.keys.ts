export const badgeKeys = {
  list: (userId: number) => ["users", userId, "badges"] as const,
};
