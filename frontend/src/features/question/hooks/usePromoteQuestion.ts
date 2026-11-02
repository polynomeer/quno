"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { questionApi } from "../api/question.api";
import { questionKeys } from "../api/question.keys";
import type { PromotionDraft } from "../api/question.types";

/**
 * What may still be internal in the draft (Phase 79.3, ADR-0086). A POST, but a pure check, so it
 * is cached per draft like a read. Pass an already debounced draft.
 */
export function usePromotionCheck(id: number, draft: PromotionDraft | null) {
  return useQuery({
    queryKey: [...questionKeys.all, id, "promotion-check", draft] as const,
    queryFn: () => questionApi.checkPromotion(id, draft as PromotionDraft),
    enabled: draft !== null,
    placeholderData: keepPreviousData,
    select: (data) => data.findings,
  });
}

export function usePromoteQuestion(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (draft: PromotionDraft) => questionApi.promote(id, draft),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: questionKeys.detail(id) });
    },
  });
}
