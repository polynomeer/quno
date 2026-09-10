"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useLiveChatMessageHistory } from "../hooks/useLiveChatMessageHistory";
import { useLiveChatSocket } from "../hooks/useLiveChatSocket";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";
import { relativeTime } from "@/shared/lib/relative-time";
import type { LiveChatMessage } from "../api/live-chat.types";

/** `@stomp/stompjs`를 실제로 끌어오는 유일한 진입점 — `LiveChatPanel`이 이 컴포넌트를
 * `next/dynamic`으로만 불러온다(quality-improvement-plan.md Q-2). "채팅 참여하기"를 누르기
 * 전까지는 이 모듈 자체가 다운로드되지 않아야, 방문자 대부분에게 안 쓰일 라이브러리를 질문 상세
 * 페이지 초기 번들에 얹지 않는다 — ADR-0039가 이미 "연결은 참여 후에만"이라고 정했지만, 지금까지는
 * 연결 시점만 늦췄을 뿐 코드 자체는 로그인한 방문자 전원에게 로드되고 있었다. */
export function LiveChatSession({
  roomId,
  questionId,
  myUserId,
}: {
  roomId: number;
  questionId: number;
  myUserId: number | undefined;
}) {
  const [draft, setDraft] = useState("");
  const { data: history } = useLiveChatMessageHistory(roomId, true);
  const { messages: liveMessages, viewerCount, status, sendMessage } = useLiveChatSocket(roomId, questionId, true);

  const allMessages = useMemo(() => {
    const seenIds = new Set((history ?? []).map((m) => m.id));
    return [...(history ?? []), ...liveMessages.filter((m) => !seenIds.has(m.id))];
  }, [history, liveMessages]);

  function handleSend() {
    if (!draft.trim()) return;
    sendMessage(draft.trim());
    setDraft("");
  }

  return (
    <div className="space-y-2">
      {viewerCount !== null && <span className="text-xs text-text-secondary">현재 {viewerCount}명이 보고 있습니다</span>}
      <ul className="max-h-80 space-y-2 overflow-y-auto rounded-md border border-border bg-surface-subtle p-3">
        {allMessages.length === 0 ? (
          <li className="text-sm text-text-secondary">{status === "connected" ? "아직 메시지가 없습니다." : "연결 중..."}</li>
        ) : (
          allMessages.map((message) => <LiveChatMessageRow key={message.id} message={message} isOwn={message.senderId === myUserId} />)
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
