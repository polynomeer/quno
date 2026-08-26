"use client";

import { useMutation } from "@tanstack/react-query";
import { questionApi } from "../api/question.api";

export function useCreateQuestion() {
  return useMutation({
    mutationFn: questionApi.create,
  });
}
