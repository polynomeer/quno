"use client";

import { useState } from "react";
import dynamic from "next/dynamic";
import { useSession } from "@/features/auth/hooks/useSession";
import { useLiveChatRoom } from "../hooks/useLiveChatRoom";
import { useOpenLiveChatRoom } from "../hooks/useOpenLiveChatRoom";
import { Button } from "@/shared/ui/Button";
import { ApiError } from "@/shared/api/api-error";

// stompjs를 실제로 쓰는 코드는 LiveChatSession 하나뿐이라, "채팅 참여하기"를 누르기 전까지는
// 그 청크 자체를 내려받지 않는다(quality-improvement-plan.md Q-2). WebSocket UI라 서버 렌더링할
// 이유도 없어 ssr: false.
const LiveChatSession = dynamic(() => import("./LiveChatSession").then((m) => m.LiveChatSession), {
  ssr: false,
  loading: () => <p className="text-sm text-text-secondary">불러오는 중...</p>,
});

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

  if (isLoading) {
    return null;
  }

  return (
    <section id="live-chat" className="space-y-3 rounded-lg border border-border p-4">
      <h2 className="text-sm font-semibold text-text-secondary">실시간 질문방</h2>

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

      {joined && room && <LiveChatSession roomId={room.id} questionId={questionId} myUserId={me?.id} />}
    </section>
  );
}
