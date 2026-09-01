/** Mirrors backend DirectAskRequestStatus/DirectAskPaymentStatus (domain/directask). */
export type DirectAskRequestStatus = "AWAITING_PAYMENT" | "PENDING" | "ACCEPTED" | "DECLINED";
export type DirectAskPaymentStatus = "PENDING" | "PAID" | "CANCELLED";

/** Mirrors DirectAskPaymentResponse — `clientKey` is Toss's public widget key. */
export interface DirectAskPayment {
  orderId: string;
  amount: number;
  status: DirectAskPaymentStatus;
  clientKey: string;
}

/** Mirrors DirectAskRequestResponse (returned by the payment confirm endpoint). */
export interface DirectAskRequest {
  id: number;
  questionId: number;
  requesterId: number;
  targetUserId: number;
  message: string | null;
  status: DirectAskRequestStatus;
  createdAt: string;
  respondedAt: string | null;
}

export interface CreateDirectAskRequestResult {
  request: DirectAskRequest;
  payment: DirectAskPayment;
}

/** Mirrors DirectAskRequestListItemResponse — denormalized for `GET /me/direct-asks`. */
export interface DirectAskRequestListItem {
  id: number;
  questionId: number;
  questionTitle: string;
  requesterId: number;
  requesterNickname: string;
  targetUserId: number;
  targetUserNickname: string;
  message: string | null;
  status: DirectAskRequestStatus;
  createdAt: string;
  respondedAt: string | null;
}
