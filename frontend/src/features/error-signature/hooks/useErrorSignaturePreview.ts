"use client";

import { useQuery } from "@tanstack/react-query";
import { errorSignatureApi } from "../api/error-signature.api";

/** Mirrors the backend's input cap (ErrorSignatureExtractor.MAX_INPUT_LENGTH). */
const MAX_LENGTH = 20_000;

/** Cheap client-side gate so ordinary prose never hits the server: the backend only finds a
 * signature where an exception class name appears anyway. */
const LOOKS_LIKE_ERROR = /[A-Z][\w$]*(?:Exception|Error|Throwable)\b/;

/** "같은 에러 N건" for the ask form (Phase 64, ADR-0075). Pass already-debounced text. */
export function useErrorSignaturePreview(text: string) {
  const input = text.slice(0, MAX_LENGTH);
  return useQuery({
    queryKey: ["error-signatures", "preview", input],
    queryFn: () => errorSignatureApi.preview(input),
    enabled: LOOKS_LIKE_ERROR.test(input),
    staleTime: 60_000,
  });
}
