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
  body: string;
  createdAt: string;
}
