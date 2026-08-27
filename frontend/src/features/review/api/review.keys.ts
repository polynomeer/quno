export const reviewKeys = {
  list: (questionId: number) => ["questions", questionId, "review-requests"] as const,
};
