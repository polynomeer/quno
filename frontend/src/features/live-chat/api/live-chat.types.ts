/** Mirrors backend LiveChatRoomResponse/LiveChatMessageResponse (interfaces/api/livechat). */
export interface LiveChatRoom {
  id: number;
  questionId: number;
  createdBy: number;
  createdAt: string;
}

/** `id` is a MongoDB ObjectId string, not a number — see LiveChatMessage (domain/livechat). */
export interface LiveChatMessage {
  id: string;
  roomId: number;
  senderId: number;
  /** Filled by the history endpoint only; live STOMP frames carry just senderId. */
  senderNickname?: string | null;
  body: string;
  createdAt: string;
}

/** Unsaved draft built from selected messages — mirrors LiveChatAnswerDraftResponse (ADR-0072). */
export interface LiveChatAnswerDraft {
  roomId: number;
  questionId: number;
  body: string;
  messageCount: number;
}
