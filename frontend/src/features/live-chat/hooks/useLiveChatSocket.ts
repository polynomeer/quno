"use client";

import { useEffect, useRef, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import { tokenStorage } from "@/shared/lib/token-storage";
import { getLiveChatWebSocketUrl } from "../lib/websocket-url";
import type { LiveChatMessage } from "../api/live-chat.types";

export type LiveChatConnectionStatus = "connecting" | "connected" | "disconnected";

interface PresenceBroadcast {
  viewerCount: number;
}

/**
 * Owns the STOMP/WebSocket connection for one room (Phase 24/26, ADR-0036) — subscribes to both
 * `/topic/live-chat/{roomId}` (messages, including the sender's own — no optimistic add needed)
 * and `/topic/questions/{questionId}/presence` (viewer count; subscribing IS what makes the
 * backend count you as a viewer, see PresenceEventListener). Connects only while `enabled` is
 * true — there's no ambient presence tracking just from having the question page open, matching
 * ADR-0038's "connect on demand" restraint for optional real-time features.
 */
export function useLiveChatSocket(roomId: number | null, questionId: number, enabled: boolean) {
  const [messages, setMessages] = useState<LiveChatMessage[]>([]);
  const [viewerCount, setViewerCount] = useState<number | null>(null);
  const [status, setStatus] = useState<LiveChatConnectionStatus>("disconnected");
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!enabled || !roomId) return;
    const accessToken = tokenStorage.getAccessToken();
    if (!accessToken) return;

    const client = new Client({
      brokerURL: getLiveChatWebSocketUrl(),
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 3000,
      // Fires right before each (re)connection attempt, asynchronously — unlike a bare setState
      // call at the top of the effect body, this doesn't trip react-hooks/set-state-in-effect.
      beforeConnect: () => setStatus("connecting"),
      onConnect: () => {
        setStatus("connected");
        client.subscribe(`/topic/live-chat/${roomId}`, (frame: IMessage) => {
          const payload = JSON.parse(frame.body) as LiveChatMessage;
          setMessages((prev) => (prev.some((m) => m.id === payload.id) ? prev : [...prev, payload]));
        });
        client.subscribe(`/topic/questions/${questionId}/presence`, (frame: IMessage) => {
          const payload = JSON.parse(frame.body) as PresenceBroadcast;
          setViewerCount(payload.viewerCount);
        });
      },
      onWebSocketClose: () => setStatus("disconnected"),
    });
    clientRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
      clientRef.current = null;
      setStatus("disconnected");
    };
  }, [enabled, roomId, questionId]);

  function sendMessage(body: string) {
    if (!roomId || status !== "connected") return;
    clientRef.current?.publish({
      destination: `/app/live-chat/${roomId}/send`,
      body: JSON.stringify({ body }),
    });
  }

  return { messages, viewerCount, status, sendMessage };
}
