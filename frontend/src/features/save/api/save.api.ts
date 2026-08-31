import { httpClient } from "@/shared/api/http-client";
import type { SavedQuestion } from "./save.types";

export const saveApi = {
  mySaves: () => httpClient.get<SavedQuestion[]>("/api/v1/me/saves"),
  save: (questionId: number) => httpClient.post<void>(`/api/v1/questions/${questionId}/save`),
  unsave: (questionId: number) => httpClient.delete<void>(`/api/v1/questions/${questionId}/save`),
};
