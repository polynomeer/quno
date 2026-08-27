export const userKeys = {
  profile: (id: number) => ["users", id, "profile"] as const,
  reputation: (id: number) => ["users", id, "reputation"] as const,
};
