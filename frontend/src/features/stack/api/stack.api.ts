import { httpClient } from "@/shared/api/http-client";
import type { ParsedDependencyFile, StackDependency, StackWardSuggestion } from "./stack.types";

export const stackApi = {
  /** Parse only — the server keeps nothing from the pasted file (ADR-0076). */
  parse: (content: string, fileName?: string) =>
    httpClient.post<ParsedDependencyFile>("/api/v1/stack-fingerprints/parse", fileName ? { fileName, content } : { content }),
  /** "내 스택" — private to the signed-in user (ADR-0078). */
  myStack: () => httpClient.get<{ dependencies: StackDependency[] }>("/api/v1/me/stack"),
  /** Replaces the whole list; `[]` clears it. */
  replaceMyStack: (dependencies: StackDependency[]) =>
    httpClient.put<{ dependencies: StackDependency[] }>("/api/v1/me/stack", { dependencies }),
  wardSuggestions: () => httpClient.get<StackWardSuggestion[]>("/api/v1/me/stack/ward-suggestions"),
};
