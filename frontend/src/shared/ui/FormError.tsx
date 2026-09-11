import { ApiError } from "@/shared/api/api-error";
import { cn } from "@/shared/lib/cn";

interface FormErrorProps {
  /** ApiError, 다른 Error, 이미 사람이 읽을 수 있는 문자열(예: 결제창 오픈 실패) 중 하나. */
  error: unknown;
  fallback: string;
  /** 주변 요소가 전부 text-xs인 컴팩트한 폼(예: 댓글 작성)에서 맞춘다. `cn`이 순수 clsx라 text-sm과
   * text-xs를 className으로 동시에 넘기면 승자가 CSS 소스 순서에 좌우되므로, 크기는 별도 prop으로 받는다. */
  size?: "xs" | "sm";
  /** 폰트 크기 외의 레이아웃 클래스(예: 여백 mt-2, 폭 w-full)만 여기로 넘긴다. */
  className?: string;
}

const sizeClasses: Record<NonNullable<FormErrorProps["size"]>, string> = {
  xs: "text-xs",
  sm: "text-sm",
};

/** 폼 제출 실패 메시지의 공통 표시 방식 — 모든 폼이 각자 베껴 쓰던 문단을 하나로 통일한다. */
export function FormError({ error, fallback, size = "sm", className }: FormErrorProps) {
  if (!error) return null;
  const message = typeof error === "string" ? error : error instanceof ApiError ? error.message : fallback;
  return (
    <p role="alert" className={cn(sizeClasses[size], "text-danger", className)}>
      {message}
    </p>
  );
}
