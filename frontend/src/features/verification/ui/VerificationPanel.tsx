"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { relativeTime } from "@/shared/lib/relative-time";
import { FormError } from "@/shared/ui/FormError";
import {
  verificationApi,
  verificationKeys,
  type QuestionClassification,
  type VerificationCheck,
  type VerificationFlag,
  type VerificationLevel,
} from "../api/verification.api";

const LEVEL: Record<VerificationLevel, string> = {
  L0: "미검증",
  L1: "구조 충족",
  L2: "재현됨",
  L3: "독립 확인",
  L4: "원인 규명",
};

const FLAG: Record<VerificationFlag, string> = {
  NOT_REPRODUCING: "최근 실행에서 재현되지 않아요. 수정됐거나 환경이 달라졌을 수 있어요.",
  SIGNATURE_MISMATCH: "실행은 실패했지만 보고된 에러와 다른 에러였어요.",
  STALE: "마지막 재현이 90일이 넘었어요. 다시 실행하면 검색 순위도 돌아와요.",
  DISPUTED: "결과에 이의가 제기돼 다음 실행을 기다리고 있어요.",
};

const CLASSIFICATION: Record<QuestionClassification, string> = {
  MISUSE: "오용",
  NOT_A_BUG: "버그 아님",
};

const L1_STEPS: { check: VerificationCheck; label: string }[] = [
  { check: "ERROR_SIGNATURE", label: "스택 트레이스를 로그나 본문에 붙이기" },
  { check: "ENVIRONMENT_SNAPSHOT", label: "환경 스냅샷 붙이기 (quno ask 또는 quno capture)" },
  { check: "EXPECTED_ACTUAL", label: "기대한 동작과 실제 동작 적기" },
  { check: "REPRO_STEPS", label: "재현 절차나 코드 블록 넣기" },
  { check: "NO_SECRETS", label: "비밀값으로 보이는 값 정리하기" },
];

const L2_STEPS: { check: VerificationCheck; label: string }[] = [
  { check: "REPRODUCTION_CAPSULE", label: "재현 캡슐 붙이기" },
  { check: "TRUSTED_REPRODUCTION", label: "Quno 워크플로로 GitHub 호스티드 러너에서 실패 확인" },
  { check: "SIGNATURE_MATCH", label: "실행 로그의 에러가 보고된 에러와 일치" },
];

/**
 * The question's level and how to raise it (Phase 94.1, 94.4, ADR-0089). L3 and L4 aren't offered:
 * they need independent confirmation and upstream links, which don't exist yet.
 */
export function VerificationPanel({ questionId, isAuthor }: { questionId: number; isAuthor: boolean }) {
  const queryClient = useQueryClient();
  const { data } = useQuery({ queryKey: verificationKeys.detail(questionId), queryFn: () => verificationApi.get(questionId) });
  const classify = useMutation({
    mutationFn: (classification: QuestionClassification | null) => verificationApi.classify(questionId, classification),
    onSuccess: (result) => queryClient.setQueryData(verificationKeys.detail(questionId), result),
  });

  if (!data) return null;
  const steps = data.level === "L0" ? L1_STEPS : L2_STEPS;
  const missing = data.level === "L2" ? [] : steps.filter((step) => !data.checks[step.check]);
  const next = data.level === "L0" ? "L1" : "L2";

  return (
    <section aria-labelledby="verification-heading" className="space-y-3 rounded-[20px] border border-border bg-surface p-5">
      <div className="flex items-center justify-between gap-2">
        <h2 id="verification-heading" className="text-sm font-semibold text-text-primary">
          검증 등급
        </h2>
        <span className={`inline-flex h-[22px] items-center rounded-full px-2 text-[11px] font-semibold ${data.level === "L2" ? "bg-success-subtle text-success" : "bg-surface-subtle text-text-secondary"}`}>
          {data.level} {LEVEL[data.level]}
        </span>
      </div>

      {data.lastReproducedAt && (
        <p className="text-xs text-text-secondary">
          마지막 재현 {relativeTime(data.lastReproducedAt)}
          {data.lastRunUrl && (
            <>
              {" · "}
              <a href={data.lastRunUrl} target="_blank" rel="noopener noreferrer" className="text-brand hover:underline">
                실행 기록
              </a>
            </>
          )}
        </p>
      )}

      {data.flags.map((flag) => (
        <p key={flag} className="rounded-xl bg-warning-subtle px-3 py-2 text-xs text-warning">
          {FLAG[flag]}
        </p>
      ))}

      {data.classification && <p className="text-xs font-semibold text-text-primary">작성자 판단: {CLASSIFICATION[data.classification]}</p>}

      {isAuthor && missing.length > 0 && (
        <div className="space-y-1.5">
          <p className="text-xs font-semibold text-text-primary">{next}로 올리려면</p>
          <ul className="space-y-1 text-xs text-text-secondary">
            {missing.map((step) => (
              <li key={step.check}>· {step.label}</li>
            ))}
          </ul>
        </div>
      )}

      {data.level === "L2" && <p className="text-xs text-text-secondary">이 실패가 존재한다는 증거예요. 답변이 맞다는 뜻은 아니에요.</p>}

      {isAuthor && (
        <div className="space-y-1">
          <label htmlFor="question-classification" className="block text-xs font-semibold text-text-primary">
            원인을 알고 보니
          </label>
          <select
            id="question-classification"
            className="w-full rounded-2xl border border-border-strong bg-surface px-3 py-2 text-sm text-text-primary"
            value={data.classification ?? ""}
            disabled={classify.isPending}
            onChange={(event) => classify.mutate((event.target.value || null) as QuestionClassification | null)}
          >
            <option value="">결함 보고</option>
            <option value="MISUSE">오용</option>
            <option value="NOT_A_BUG">버그 아님</option>
          </select>
          <FormError error={classify.error} fallback="분류를 저장하지 못했습니다." size="xs" />
        </div>
      )}
    </section>
  );
}
