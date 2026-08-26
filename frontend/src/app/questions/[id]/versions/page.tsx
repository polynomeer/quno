"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useQuestionVersions } from "@/features/question/hooks/useQuestionVersions";
import { useQuestionDiff } from "@/features/question/hooks/useQuestionDiff";
import { DiffView } from "@/features/question/ui/DiffView";
import { relativeTime } from "@/shared/lib/relative-time";
import { Skeleton } from "@/shared/ui/Skeleton";

export default function QuestionVersionHistoryPage({ params }: PageProps<"/questions/[id]/versions">) {
  const { id } = use(params);
  const questionId = Number(id);
  const { isLoading: authLoading } = useRequireAuth();
  const { data: versions, isLoading } = useQuestionVersions(questionId);

  const [toOverride, setToOverride] = useState<number | null>(null);
  const [fromOverride, setFromOverride] = useState<number | null>(null);

  const latest = versions && versions.length > 0 ? Math.max(...versions.map((v) => v.versionNumber)) : null;
  const toVersion = toOverride ?? latest;
  const fromVersion = fromOverride ?? (latest !== null ? Math.max(1, latest - 1) : null);

  const canDiff = toVersion !== null && fromVersion !== null && fromVersion < toVersion;
  const { data: diff, isLoading: diffLoading } = useQuestionDiff(
    questionId,
    toVersion ?? 0,
    fromVersion ?? 0,
    canDiff,
  );

  if (authLoading || isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  return (
    <div className="space-y-6">
      <div>
        <Link href={`/questions/${questionId}`} className="text-sm text-text-secondary hover:underline">
          ← 질문으로 돌아가기
        </Link>
        <h1 className="mt-2 text-xl font-semibold">Revision History</h1>
      </div>

      <ul className="space-y-2">
        {(versions ?? [])
          .slice()
          .sort((a, b) => b.versionNumber - a.versionNumber)
          .map((version) => (
            <li key={version.versionNumber} className="rounded-md border border-border p-3 text-sm">
              <span className="font-medium">revision {version.versionNumber}</span> · {version.title}
              <span className="ml-2 text-text-secondary">
                by 사용자 #{version.createdBy} · {relativeTime(version.createdAt)}
              </span>
            </li>
          ))}
      </ul>

      {versions && versions.length < 2 ? (
        <p className="text-sm text-text-secondary">비교할 이전 리비전이 없습니다.</p>
      ) : (
        <section className="space-y-3">
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <label className="flex items-center gap-1">
              From
              <select
                className="rounded-md border border-border bg-surface px-2 py-1"
                value={fromVersion ?? ""}
                onChange={(event) => setFromOverride(Number(event.target.value))}
              >
                {(versions ?? []).map((v) => (
                  <option key={v.versionNumber} value={v.versionNumber}>
                    v{v.versionNumber}
                  </option>
                ))}
              </select>
            </label>
            <label className="flex items-center gap-1">
              To
              <select
                className="rounded-md border border-border bg-surface px-2 py-1"
                value={toVersion ?? ""}
                onChange={(event) => setToOverride(Number(event.target.value))}
              >
                {(versions ?? []).map((v) => (
                  <option key={v.versionNumber} value={v.versionNumber}>
                    v{v.versionNumber}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {!canDiff && <p className="text-sm text-text-secondary">From 버전은 To 버전보다 이전이어야 합니다.</p>}
          {canDiff && diffLoading && <Skeleton className="h-24 w-full" />}
          {canDiff && diff && <DiffView lines={diff.lines} />}
        </section>
      )}
    </div>
  );
}
