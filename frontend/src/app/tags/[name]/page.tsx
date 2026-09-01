"use client";

import { Suspense, use } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useTagByName } from "@/entities/tag/hooks/useTagByName";
import { useTagQuestions } from "@/entities/tag/hooks/useTagQuestions";
import { useTagContributors } from "@/entities/tag/hooks/useTagContributors";
import { useRelatedTags } from "@/entities/tag/hooks/useRelatedTags";
import { FollowTagButton } from "@/features/tag/ui/FollowTagButton";
import { TagDetailsEditor } from "@/features/tag/ui/TagDetailsEditor";
import { TagContributorList } from "@/features/tag/ui/TagContributorList";
import { RelatedTagList } from "@/features/tag/ui/RelatedTagList";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { Skeleton } from "@/shared/ui/Skeleton";
import type { TagQuestionSort } from "@/entities/tag/model/tag.types";

const sortLabels: Record<TagQuestionSort, string> = {
  latest: "Latest",
  unanswered: "Unanswered",
  top: "Top",
};

function TagDetailContent({ tagName }: { tagName: string }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const sort: TagQuestionSort =
    searchParams.get("sort") === "unanswered" ? "unanswered" : searchParams.get("sort") === "top" ? "top" : "latest";

  const { isLoading: authLoading } = useRequireAuth();
  const { data: tag, isLoading: tagLoading } = useTagByName(tagName);
  const tagId = tag?.id ?? null;
  const { data: questions, isLoading: questionsLoading } = useTagQuestions(tagId, sort);
  const { data: contributors } = useTagContributors(tagId);
  const { data: relatedTags } = useRelatedTags(tagId);

  if (authLoading || tagLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  if (!tag) {
    return <p className="text-sm text-danger">태그를 찾을 수 없습니다.</p>;
  }

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_240px]">
      <div className="space-y-6">
        <header className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h1 className="text-xl font-semibold">#{tag.name}</h1>
            <FollowTagButton tagId={tag.id} />
          </div>
          <TagDetailsEditor tag={tag} />
        </header>

        <div className="flex gap-2 border-b border-border pb-2">
          {(Object.keys(sortLabels) as TagQuestionSort[]).map((option) => (
            <button
              key={option}
              onClick={() => router.push(`/tags/${encodeURIComponent(tagName)}?sort=${option}`)}
              className={
                sort === option
                  ? "rounded-md bg-surface-subtle px-3 py-1 text-sm font-medium text-text-primary"
                  : "rounded-md px-3 py-1 text-sm text-text-secondary hover:text-text-primary"
              }
            >
              {sortLabels[option]}
            </button>
          ))}
        </div>

        {questionsLoading ? (
          <Skeleton className="h-40 w-full" />
        ) : (
          <QuestionList questions={questions ?? []} emptyMessage="이 태그가 달린 질문이 없습니다." />
        )}
      </div>

      <aside className="space-y-6">
        <section className="space-y-2">
          <h2 className="text-sm font-semibold text-text-secondary">상위 기여자</h2>
          <TagContributorList contributors={contributors ?? []} />
        </section>
        <section className="space-y-2">
          <h2 className="text-sm font-semibold text-text-secondary">관련 태그</h2>
          <RelatedTagList tags={relatedTags ?? []} />
        </section>
      </aside>
    </div>
  );
}

export default function TagDetailPage({ params }: PageProps<"/tags/[name]">) {
  const { name } = use(params);
  const tagName = decodeURIComponent(name);

  return (
    <Suspense>
      <TagDetailContent tagName={tagName} />
    </Suspense>
  );
}
