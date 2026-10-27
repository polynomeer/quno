import type { QuestionSummary } from "@/features/question/api/question.types";

/** Mirrors backend ErrorSignatureResponse (interfaces/api/errorsignature, ADR-0075). */
export interface ErrorSignature {
  exceptionClass: string;
  normalizedMessage: string;
  frames: string[];
  /** Null when the pasted text had no stack frames — then only messageKey can match. */
  signatureHash: string | null;
  messageKey: string;
}

export interface ErrorSignaturePreview {
  signatures: ErrorSignature[];
  /** Questions with the same error, signature-hash matches first. */
  questions: QuestionSummary[];
}
