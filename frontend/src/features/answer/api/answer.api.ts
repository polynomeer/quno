import { httpClient } from "@/shared/api/http-client";
import type { Answer } from "./answer.types";

export const answerApi = {
  list: (questionId: number) => httpClient.get<Answer[]>(`/api/v1/questions/${questionId}/answers`),
};
