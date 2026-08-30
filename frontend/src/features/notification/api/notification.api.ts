import { httpClient } from "@/shared/api/http-client";
import type { Notification } from "./notification.types";

export const notificationApi = {
  list: () => httpClient.get<Notification[]>("/api/v1/me/notifications"),
  markAllRead: () => httpClient.post<void>("/api/v1/me/notifications/mark-read"),
};
