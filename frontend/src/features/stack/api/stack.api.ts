import { httpClient } from "@/shared/api/http-client";
import type { ParsedDependencyFile } from "./stack.types";

export const stackApi = {
  /** Parse only — the server keeps nothing from the pasted file (ADR-0076). */
  parse: (content: string, fileName?: string) =>
    httpClient.post<ParsedDependencyFile>("/api/v1/stack-fingerprints/parse", fileName ? { fileName, content } : { content }),
};
