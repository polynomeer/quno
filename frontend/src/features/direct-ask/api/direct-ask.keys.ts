export const directAskKeys = {
  mine: (role: "sent" | "received") => ["direct-asks", "me", role] as const,
};
