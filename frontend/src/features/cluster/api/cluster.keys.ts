export const clusterKeys = {
  forQuestion: (questionId: number) => ["questions", questionId, "cluster"] as const,
};
