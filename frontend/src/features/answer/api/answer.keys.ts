export const answerKeys = {
  list: (questionId: number) => ["questions", questionId, "answers"] as const,
  versions: (answerId: number) => ["answers", answerId, "versions"] as const,
  diff: (answerId: number, version: number, from?: number) =>
    ["answers", answerId, "versions", version, "diff", from ?? "default"] as const,
};
