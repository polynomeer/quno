import { httpClient } from "@/shared/api/http-client";
import type { LiveChatMessage, LiveChatRoom } from "./live-chat.types";

export const liveChatApi = {
  openRoom: (questionId: number) => httpClient.post<LiveChatRoom>(`/api/v1/questions/${questionId}/live-chat`),
  getRoom: (questionId: number) => httpClient.get<LiveChatRoom>(`/api/v1/questions/${questionId}/live-chat`),
  messages: (roomId: number, limit = 50) =>
    httpClient.get<LiveChatMessage[]>(`/api/v1/live-chat/${roomId}/messages?limit=${limit}`),
};
