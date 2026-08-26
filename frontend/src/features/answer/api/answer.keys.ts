export const answerKeys = {
  list: (questionId: number) => ["questions", questionId, "answers"] as const,
};
