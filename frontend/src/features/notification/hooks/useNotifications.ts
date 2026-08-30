"use client";

import { useQuery } from "@tanstack/react-query";
import { notificationApi } from "../api/notification.api";
import { notificationKeys } from "../api/notification.keys";

export function useNotifications(enabled: boolean) {
  return useQuery({
    queryKey: notificationKeys.mine,
    queryFn: notificationApi.list,
    enabled,
  });
}
