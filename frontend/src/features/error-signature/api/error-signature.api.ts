import { httpClient } from "@/shared/api/http-client";
import type { ErrorSignaturePreview } from "./error-signature.types";

export const errorSignatureApi = {
  /** POST, not GET: a stack trace doesn't belong in a URL. Nothing is stored server-side. */
  preview: (text: string) => httpClient.post<ErrorSignaturePreview>("/api/v1/error-signatures/preview", { text }),
};
