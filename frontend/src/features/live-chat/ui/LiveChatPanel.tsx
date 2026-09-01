"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useSession } from "@/features/auth/hooks/useSession";
import { useLiveChatRoom } from "../hooks/useLiveChatRoom";
import { useOpenLiveChatRoom } from "../hooks/useOpenLiveChatRoom";
import { useLiveChatMessageHistory } from "../hooks/useLiveChatMessageHistory";
import { useLiveChatSocket } from "../hooks/useLiveChatSocket";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";
import { relativeTime } from "@/shared/lib/relative-time";
import { ApiError } from "@/shared/api/api-error";
import type { LiveChatMessage } from "../api/live-chat.types";

/**
 * Live Chat (Phase 24/26, ADR-0036/ADR-0038) — connects on demand, not on page load: clicking
 * "채팅 참여하기" is what opens the WebSocket and (via the presence subscription) counts you as a
 * viewer. Messages show `사용자 #{senderId}` rather than a nickname — LiveChatMessageResponse has
 * no nickname, and resolving one per message would add a DB round-trip to the hot send path
 * (unlike the Direct Ask list's one-time denormalization) — an accepted simplification, not an
 * oversight.
 */
export function LiveChatPanel({ questionId }: { questionId: number }) {
  const { data: me } = useSession();
  const { data: room, isLoading } = useLiveChatRoom(questionId);
  const openRoom = useOpenLiveChatRoom(questionId);
  const [joined, setJoined] = useState(false);
  const [draft, setDraft] = useState("");

  const roomId = room?.id ?? null;
  const { data: history } = useLiveChatMessageHistory(roomId, joined);
  const { messages: liveMessages, viewerCount, status, sendMessage } = useLiveChatSocket(roomId, questionId, joined);

  const allMessages = useMemo(() => {
    const seenIds = new Set((history ?? []).map((m) => m.id));
    return [...(history ?? []), ...liveMessages.filter((m) => !seenIds.has(m.id))];
  }, [history, liveMessages]);

  function handleSend() {
    if (!draft.trim()) return;
    sendMessage(draft.trim());
    setDraft("");
  }

  if (isLoading) {
    return null;
  }

  return (
    <section id="live-chat" className="space-y-3 rounded-lg border border-border p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-sm font-semibold text-text-secondary">실시간 질문방</h2>
        {joined && viewerCount !== null && (
          <span className="text-xs text-text-secondary">현재 {viewerCount}명이 보고 있습니다</span>
        )}
      </div>

      {!room && !joined && (
        <Button
          variant="secondary"
          onClick={() => openRoom.mutate(undefined, { onSuccess: () => setJoined(true) })}
          disabled={openRoom.isPending}
        >
          {openRoom.isPending ? "여는 중..." : "실시간 질문방 시작하기"}
        </Button>
      )}
      {openRoom.isError && (
        <p className="text-sm text-danger">
          {openRoom.error instanceof ApiError ? openRoom.error.message : "질문방을 열지 못했습니다."}
        </p>
      )}

      {room && !joined && (
        <Button variant="secondary" onClick={() => setJoined(true)}>
          채팅 참여하기
        </Button>
      )}

      {joined && (
        <div className="space-y-2">
          <ul className="max-h-80 space-y-2 overflow-y-auto rounded-md border border-border bg-surface-subtle p-3">
            {allMessages.length === 0 ? (
              <li className="text-sm text-text-secondary">
                {status === "connected" ? "아직 메시지가 없습니다." : "연결 중..."}
              </li>
            ) : (
              allMessages.map((message) => <LiveChatMessageRow key={message.id} message={message} isOwn={message.senderId === me?.id} />)
            )}
          </ul>
          <div className="flex gap-2">
            <Input
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  handleSend();
                }
              }}
              placeholder={status === "connected" ? "메시지 입력..." : "연결 중..."}
              disabled={status !== "connected"}
              maxLength={2000}
            />
            <Button onClick={handleSend} disabled={status !== "connected" || !draft.trim()}>
              보내기
            </Button>
          </div>
        </div>
      )}
    </section>
  );
}

function LiveChatMessageRow({ message, isOwn }: { message: LiveChatMessage; isOwn: boolean }) {
  return (
    <li className="text-sm">
      <div className="flex items-baseline gap-2">
        <Link href={`/users/${message.senderId}`} className="font-medium hover:underline">
          {isOwn ? "나" : `사용자 #${message.senderId}`}
        </Link>
        <span className="text-xs text-text-secondary">{relativeTime(message.createdAt)}</span>
      </div>
      <p className="whitespace-pre-wrap">{message.body}</p>
    </li>
  );
}
