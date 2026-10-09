"use client";

import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { questionApi } from "../api/question.api";
import { questionKeys } from "../api/question.keys";

/**
 * Records one view when the detail page mounts (ADR-0063). Counting lives in this explicit call,
 * not in `GET /questions/{id}`, because the server-side `generateMetadata` and crawlers hit that GET
 * too. The ref keeps React StrictMode's double-mount from sending twice (the server would dedupe
 * it anyway). A failure is ignored — a missed view count must never break reading the page.
 */
export function useRecordQuestionView(questionId: number) {
  const queryClient = useQueryClient();
  const recordedFor = useRef<number | null>(null);

  useEffect(() => {
    if (recordedFor.current === questionId) return;
    recordedFor.current = questionId;
    questionApi
      .recordView(questionId)
      .then(() => queryClient.invalidateQueries({ queryKey: questionKeys.detail(questionId) }))
      .catch(() => {});
  }, [questionId, queryClient]);
}
