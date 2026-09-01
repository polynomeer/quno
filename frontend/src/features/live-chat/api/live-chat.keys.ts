export const liveChatKeys = {
  room: (questionId: number) => ["live-chat", "room", questionId] as const,
  messages: (roomId: number) => ["live-chat", "messages", roomId] as const,
};
