"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useLiveChatRoom } from "@/features/live-chat/hooks/useLiveChatRoom";
import { useLiveChatMessageHistory } from "@/features/live-chat/hooks/useLiveChatMessageHistory";
import { useLiveChatAnswerDraft } from "@/features/live-chat/hooks/useLiveChatAnswerDraft";
import { useCreateAnswerFromLiveChat } from "@/features/answer/hooks/useCreateAnswerFromLiveChat";
import { Button } from "@/shared/ui/Button";
import { EmptyState } from "@/shared/ui/EmptyState";
import { FormError } from "@/shared/ui/FormError";
import { MarkdownEditor } from "@/shared/ui/MarkdownEditor";
import { Skeleton } from "@/shared/ui/Skeleton";
import { relativeTime } from "@/shared/lib/relative-time";
import { cn } from "@/shared/lib/cn";

/**
 * Phase 71 (ADR-0073): pick the messages that solved it, build a draft on the server, edit it,
 * and publish it as an ordinary answer that cites the room. Layout follows the "질문방 → 답변으로
 * 게시" artboard of web design v2 (ADR-0071).
 */
export default function LiveChatToAnswerPage({
  params,
}: PageProps<"/questions/[id]/live-chat/answer">) {
  const { id } = use(params);
  const questionId = Number(id);
  const router = useRouter();
  const { me, isLoading: authLoading } = useRequireAuth();
  const { data: room, isLoading: roomLoading } = useLiveChatRoom(questionId);
  const roomId = room?.id ?? null;
  const { data: messages, isLoading: messagesLoading } =
    useLiveChatMessageHistory(roomId, Boolean(me));
  const draft = useLiveChatAnswerDraft(roomId);
  const publish = useCreateAnswerFromLiveChat(questionId, roomId);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [body, setBody] = useState("");

  if (authLoading || roomLoading || (roomId !== null && messagesLoading)) {
    return <Skeleton className="h-60 w-full" />;
  }

  const questionHref = `/questions/${questionId}`;

  if (!room) {
    return <EmptyState message="이 질문에는 아직 실시간 질문방이 없습니다." />;
  }

  function toggle(messageId: string) {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(messageId)) next.delete(messageId);
      else next.add(messageId);
      return next;
    });
  }

  function buildDraft() {
    draft.mutate([...selected], {
      onSuccess: (result) => setBody(result.body),
    });
  }

  function handlePublish() {
    publish.mutate(body, {
      onSuccess: (answer) => router.push(`${questionHref}#answer-${answer.id}`),
    });
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="inline-flex items-center gap-2 text-[13px] font-bold text-success">
            <span
              aria-hidden="true"
              className="size-2 rounded-full bg-live ring-4 ring-live-ring"
            />
            실시간 질문방
          </p>
          <h1 className="mt-1 text-[28px] leading-tight font-bold tracking-tight">
            대화를 답변으로 정리하기
          </h1>
          <p className="mt-1 text-sm text-text-secondary">
            남길 메시지를 고르고 초안을 만든 뒤, 다듬어서 답변으로 게시하세요.
          </p>
        </div>
        <Link
          href={questionHref}
          className="inline-flex h-11 items-center rounded-full bg-surface-subtle px-5 text-sm font-semibold text-text-primary hover:bg-border"
        >
          질문으로 돌아가기
        </Link>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section
          aria-labelledby="live-chat-messages-heading"
          className="space-y-2 rounded-3xl border border-border bg-surface p-4 sm:p-5"
        >
          <div className="flex items-center justify-between px-1 pb-1">
            <h2
              id="live-chat-messages-heading"
              className="text-[15px] font-bold"
            >
              메시지 {selected.size}개 선택
            </h2>
            <span className="text-xs text-text-secondary">
              최근 {messages?.length ?? 0}개
            </span>
          </div>
          {(messages ?? []).length === 0 ? (
            <EmptyState
              message="아직 메시지가 없습니다."
              inline
              className="px-1"
            />
          ) : (
            <ul className="space-y-1.5">
              {(messages ?? []).map((message) => {
                const checked = selected.has(message.id);
                return (
                  <li key={message.id}>
                    <label
                      className={cn(
                        "flex cursor-pointer items-start gap-3 rounded-2xl px-3.5 py-3 transition-colors",
                        checked ? "bg-brand-subtle" : "hover:bg-surface-subtle",
                      )}
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggle(message.id)}
                        className="mt-1 size-[18px] shrink-0 accent-brand"
                      />
                      <span className="min-w-0 flex-1">
                        <span className="block text-xs font-bold text-text-secondary">
                          {message.senderNickname ??
                            `사용자 #${message.senderId}`}{" "}
                          · {relativeTime(message.createdAt)}
                        </span>
                        <span className="mt-0.5 block text-sm whitespace-pre-wrap text-text-primary">
                          {message.body}
                        </span>
                      </span>
                    </label>
                  </li>
                );
              })}
            </ul>
          )}
        </section>

        <section
          aria-labelledby="live-chat-draft-heading"
          className="space-y-4 rounded-3xl border-2 border-brand bg-surface p-5 sm:p-6"
        >
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 id="live-chat-draft-heading" className="text-[15px] font-bold">
              답변 초안
            </h2>
            <Button
              variant="secondary"
              onClick={buildDraft}
              disabled={selected.size === 0 || draft.isPending}
            >
              {draft.isPending
                ? "만드는 중..."
                : `선택한 메시지로 초안 만들기 (${selected.size})`}
            </Button>
          </div>
          <FormError
            error={draft.error}
            fallback="초안을 만들지 못했습니다. 질문방 참여자만 정리할 수 있습니다."
          />
          <label htmlFor="live-chat-draft" className="sr-only">
            답변 초안
          </label>
          <MarkdownEditor
            id="live-chat-draft"
            value={body}
            onChange={setBody}
            rows={14}
            placeholder="왼쪽에서 메시지를 고르고 초안을 만들면 여기에 채워집니다."
          />
          <FormError
            error={publish.error}
            fallback="답변을 게시하지 못했습니다."
          />
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-4">
            <p className="text-xs text-text-secondary">
              내 이름으로 게시돼요. 채택은 질문 작성자가 해요.
            </p>
            <Button
              onClick={handlePublish}
              disabled={!body.trim() || publish.isPending}
            >
              {publish.isPending ? "게시 중..." : "답변으로 게시"}
            </Button>
          </div>
        </section>
      </div>
    </div>
  );
}
