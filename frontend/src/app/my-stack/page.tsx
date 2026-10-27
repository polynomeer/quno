"use client";

import { useState } from "react";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import type { StackDependency } from "@/features/stack/api/stack.types";
import { useMyStack, useReplaceMyStack, useStackWardSuggestions } from "@/features/stack/hooks/useMyStack";
import { DependencyChips } from "@/features/stack/ui/DependencyChips";
import { StackFingerprintInput } from "@/features/stack/ui/StackFingerprintInput";
import { StackWardSuggestions } from "@/features/stack/ui/StackWardSuggestions";
import { Button } from "@/shared/ui/Button";
import { FormError } from "@/shared/ui/FormError";
import { Skeleton } from "@/shared/ui/Skeleton";

/**
 * "내 스택" (Phase 63.5, ADR-0078): the dependencies of the project I work on, visible only to me,
 * and the questions sharing them that I might want to Ward. The richer stack lens and digest
 * screens of web design v2 belong to Phase 73 and 77.
 */
export default function MyStackPage() {
  const { me, isLoading: authLoading } = useRequireAuth();
  const { data: stack, isLoading } = useMyStack(Boolean(me));
  const { data: suggestions } = useStackWardSuggestions(Boolean(me) && (stack?.length ?? 0) > 0);
  const replace = useReplaceMyStack();
  const [picked, setPicked] = useState<StackDependency[]>([]);
  const [inputKey, setInputKey] = useState(0);

  if (authLoading || isLoading) {
    return <Skeleton className="h-60 w-full" />;
  }

  function save(dependencies: StackDependency[]) {
    replace.mutate(dependencies, {
      onSuccess: () => {
        setPicked([]);
        setInputKey((key) => key + 1);
      },
    });
  }

  const current = stack ?? [];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-[32px] leading-10 font-bold tracking-tight">내 스택</h1>
        <p className="mt-1 text-sm text-text-secondary">
          작업 중인 프로젝트의 의존성을 저장해 두면, 같은 패키지를 쓰는 질문을 Watch 후보로 보여드려요. 나만 볼 수 있어요.
        </p>
      </div>

      <section aria-labelledby="my-stack-current" className="space-y-3 rounded-3xl border border-border bg-surface p-5 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 id="my-stack-current" className="text-[15px] font-bold">
            저장된 스택 {current.length}개
          </h2>
          {current.length > 0 && (
            <Button variant="ghost" onClick={() => save([])} disabled={replace.isPending}>
              모두 지우기
            </Button>
          )}
        </div>
        {current.length === 0 ? (
          <p className="text-sm text-text-secondary">아직 저장된 스택이 없어요. 아래에 의존성 파일을 붙여 넣어 주세요.</p>
        ) : (
          <DependencyChips dependencies={current} />
        )}
      </section>

      <section aria-labelledby="my-stack-replace" className="space-y-4 rounded-3xl border border-border bg-surface p-5 sm:p-6">
        <h2 id="my-stack-replace" className="text-[15px] font-bold">
          의존성 파일로 바꾸기
        </h2>
        <StackFingerprintInput
          key={inputKey}
          onChange={setPicked}
          hint="build.gradle.kts·pom.xml·package.json 내용을 붙여 넣으면 의존성을 읽어요. 원본 파일은 저장되지 않고, 체크한 항목으로 내 스택을 통째로 바꿔요."
        />
        <FormError error={replace.error} fallback="스택을 저장하지 못했습니다." />
        <Button onClick={() => save(picked)} disabled={picked.length === 0 || replace.isPending}>
          {replace.isPending ? "저장 중..." : `선택한 ${picked.length}개로 저장`}
        </Button>
      </section>

      {current.length > 0 && (
        <section aria-labelledby="my-stack-suggestions" className="space-y-3">
          <h2 id="my-stack-suggestions" className="text-xl font-bold">
            내 스택과 겹치는 질문
          </h2>
          <p className="text-sm text-text-secondary">
            패키지가 많이 겹치는 순서예요. 고른 질문만 Watch하고, 앞으로 올라올 질문을 자동으로 Watch하지는 않아요.
          </p>
          <StackWardSuggestions suggestions={suggestions ?? []} />
        </section>
      )}
    </div>
  );
}
