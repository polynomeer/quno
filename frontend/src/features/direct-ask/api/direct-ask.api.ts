import { httpClient } from "@/shared/api/http-client";
import type {
  CreateDirectAskRequestResult,
  DirectAskRequest,
  DirectAskRequestListItem,
} from "./direct-ask.types";

export const directAskApi = {
  create: (questionId: number, targetUserId: number, message: string | undefined) =>
    httpClient.post<CreateDirectAskRequestResult>(`/api/v1/questions/${questionId}/direct-asks`, {
      targetUserId,
      message,
    }),
  confirmPayment: (orderId: string, paymentKey: string, amount: number) =>
    httpClient.post<DirectAskRequest>("/api/v1/direct-asks/payments/confirm", { orderId, paymentKey, amount }),
  accept: (id: number) => httpClient.post<void>(`/api/v1/direct-asks/${id}/accept`),
  decline: (id: number) => httpClient.post<void>(`/api/v1/direct-asks/${id}/decline`),
  mine: (role: "sent" | "received") =>
    httpClient.get<DirectAskRequestListItem[]>(`/api/v1/me/direct-asks?role=${role}`),
};
