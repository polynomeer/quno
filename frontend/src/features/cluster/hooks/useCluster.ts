"use client";

import { useQuery } from "@tanstack/react-query";
import { clusterApi } from "../api/cluster.api";
import { clusterKeys } from "../api/cluster.keys";
import { ApiError } from "@/shared/api/api-error";

/** No cluster is a normal state (backend 404s `QuestionNotInAnyClusterException`), not an error —
 * resolved to `null` here so the panel can just hide itself instead of showing an error banner. */
export function useCluster(questionId: number) {
  return useQuery({
    queryKey: clusterKeys.forQuestion(questionId),
    queryFn: async () => {
      try {
        return await clusterApi.getForQuestion(questionId);
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
          return null;
        }
        throw error;
      }
    },
  });
}
