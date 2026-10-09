"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { z } from "zod";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useCreateQuestion } from "@/features/question/hooks/useCreateQuestion";
import { TagInput } from "@/features/question/ui/TagInput";
import { useSearch } from "@/features/search/hooks/useSearch";
import { useDebouncedValue } from "@/shared/hooks/useDebouncedValue";
import { Input } from "@/shared/ui/Input";
import { Textarea } from "@/shared/ui/Textarea";
import { MarkdownEditor } from "@/shared/ui/MarkdownEditor";
import { Button } from "@/shared/ui/Button";
import { Skeleton } from "@/shared/ui/Skeleton";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { FormError } from "@/shared/ui/FormError";
import { useLocale } from "@/shared/i18n/LocaleProvider";
import type { Dictionary } from "@/shared/i18n/dictionary";

function buildAskSchema(t: Dictionary) {
  return z.object({
    title: z.string().trim().min(1, t.ask.titleRequired).max(300, t.ask.titleTooLong),
    body: z.string().trim().min(1, t.ask.bodyRequired),
    environment: z.string().optional(),
    logs: z.string().optional(),
  });
}

type AskFormValues = z.infer<ReturnType<typeof buildAskSchema>>;

const DRAFT_KEY = "quno:ask-draft";

interface AskDraft extends AskFormValues {
  tags: string[];
}

export default function AskPage() {
  const { t } = useLocale();
  const { isLoading: authLoading } = useRequireAuth();
  const router = useRouter();
  const createQuestion = useCreateQuestion();
  const [tags, setTags] = useState<string[]>([]);
  const askSchema = useMemo(() => buildAskSchema(t), [t]);

  const {
    register,
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<AskFormValues>({
    resolver: zodResolver(askSchema),
    defaultValues: { title: "", body: "", environment: "", logs: "" },
  });

  // Restore a draft after mount — doing this in an effect (not defaultValues) avoids a
  // server/client mismatch, since the server never has access to localStorage.
  useEffect(() => {
    try {
      const raw = localStorage.getItem(DRAFT_KEY);
      if (raw) {
        const draft = JSON.parse(raw) as AskDraft;
        reset({ title: draft.title, body: draft.body, environment: draft.environment, logs: draft.logs });
        setTags(draft.tags ?? []);
      }
    } catch {
      // corrupt or unavailable draft — start blank
    }
  }, [reset]);

  const watched = watch();
  useEffect(() => {
    const timer = setTimeout(() => {
      try {
        localStorage.setItem(DRAFT_KEY, JSON.stringify({ ...watched, tags }));
      } catch {
        // storage unavailable/full — draft just won't persist
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [watched, tags]);

  const debouncedTitle = useDebouncedValue(watched.title, 400);
  const similarQuery = debouncedTitle.trim().length >= 3 ? debouncedTitle.trim() : "";
  const { data: similarQuestions } = useSearch(similarQuery);

  async function onSubmit(values: AskFormValues) {
    try {
      const result = await createQuestion.mutateAsync({
        title: values.title.trim(),
        body: values.body.trim(),
        environment: values.environment?.trim() || undefined,
        logs: values.logs?.trim() || undefined,
        tags,
      });
      try {
        localStorage.removeItem(DRAFT_KEY);
      } catch {
        // ignore
      }
      router.push(`/questions/${result.id}`);
    } catch {
      // error surfaced below via createQuestion.error
    }
  }

  if (authLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  // Suggestions, not blocking errors (design.md #11 Quality Check) — derived from what's typed.
  const checklist = [
    { label: t.ask.checklistTitle, done: watched.title.trim().length >= 15 },
    { label: t.ask.checklistEnvironment, done: Boolean(watched.environment?.trim()) },
    { label: t.ask.checklistCode, done: watched.body.includes("```") },
    { label: t.ask.checklistLogs, done: Boolean(watched.logs?.trim()) },
    { label: t.ask.checklistTags, done: tags.length > 0 },
  ];
  const doneCount = checklist.filter((item) => item.done).length;

  return (
    <div className="grid grid-cols-1 gap-10 lg:grid-cols-[minmax(0,1fr)_300px]">
      <form onSubmit={handleSubmit(onSubmit)} className="min-w-0 space-y-7">
        <div>
          <h1 className="text-[28px] font-bold tracking-tight">{t.ask.heading}</h1>
          <p className="mt-1.5 text-text-body">{t.ask.subtitle}</p>
        </div>

        <div className="space-y-2">
          <div className="flex items-baseline justify-between">
            <label htmlFor="ask-title" className="font-semibold">
              {t.ask.titleLabel}
            </label>
            <span className="font-mono text-xs text-text-secondary">{watched.title.length} / 300</span>
          </div>
          <Input
            id="ask-title"
            placeholder={t.ask.titlePlaceholder}
            className="h-12 text-base"
            {...register("title")}
          />
          {errors.title && <p className="mt-1 text-sm text-danger">{errors.title.message}</p>}
        </div>

        {/* Similar questions sit right under the title — search first, ask second (design.md #2). */}
        <section aria-label={t.ask.similarQuestions} className="overflow-hidden rounded-xl border border-border bg-surface">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border bg-canvas px-4 py-3">
            <h2 className="text-sm font-semibold">{t.ask.similarHeading}</h2>
            {similarQuery && similarQuestions && (
              <span className="font-mono text-xs text-text-secondary">{similarQuestions.length}</span>
            )}
          </div>
          <div className="p-3">
            {similarQuery ? (
              <QuestionList questions={similarQuestions ?? []} emptyMessage={t.ask.noSimilarQuestions} />
            ) : (
              <p className="px-1 text-sm text-text-secondary">{t.ask.similarQuestionsHint}</p>
            )}
          </div>
        </section>

        <div className="space-y-2">
          <label htmlFor="ask-body" className="block font-semibold">
            {t.ask.bodyLabel}
          </label>
          <Controller
            control={control}
            name="body"
            render={({ field }) => (
              <MarkdownEditor
                id="ask-body"
                value={field.value}
                onChange={field.onChange}
                rows={12}
                placeholder={t.ask.bodyPlaceholder}
              />
            )}
          />
          {errors.body && <p className="mt-1 text-sm text-danger">{errors.body.message}</p>}
        </div>

        <div className="space-y-2">
          <label htmlFor="ask-environment" className="block font-semibold">
            {t.ask.environmentLabel}
          </label>
          <Textarea id="ask-environment" rows={2} placeholder={t.ask.environmentPlaceholder} {...register("environment")} />
        </div>

        <div className="space-y-2">
          <label htmlFor="ask-logs" className="block font-semibold">
            {t.ask.logsLabel}
          </label>
          <Textarea
            id="ask-logs"
            rows={4}
            placeholder={t.ask.logsPlaceholder}
            className="font-mono text-[13px]"
            {...register("logs")}
          />
        </div>

        <div className="space-y-2">
          <span className="block font-semibold">{t.ask.tagsLabel}</span>
          <TagInput value={tags} onChange={setTags} />
        </div>

        <FormError error={createQuestion.error} fallback={t.ask.failed} />

        <div className="flex justify-end border-t border-border pt-4">
          <Button type="submit" className="h-11 px-6" disabled={createQuestion.isPending}>
            {createQuestion.isPending ? t.ask.submitting : t.ask.submit}
          </Button>
        </div>
      </form>

      <aside>
        <section className="rounded-xl border border-border bg-surface p-5 lg:sticky lg:top-24">
          <div className="mb-4 flex items-baseline justify-between">
            <h2 className="text-[15px] font-semibold">{t.ask.checklistHeading}</h2>
            <span className="font-mono text-xs text-success">
              {doneCount} / {checklist.length}
            </span>
          </div>
          <ul className="space-y-3 text-sm">
            {checklist.map((item) => (
              <li key={item.label} className="flex items-center gap-2.5">
                <span
                  aria-hidden="true"
                  className={
                    item.done
                      ? "grid size-5 shrink-0 place-items-center rounded-full bg-success-strong text-[11px] font-bold text-surface"
                      : "size-5 shrink-0 rounded-full border-[1.5px] border-border-strong"
                  }
                >
                  {item.done ? "✓" : null}
                </span>
                <span className={item.done ? "text-text-secondary line-through" : "font-medium text-text-primary"}>
                  {item.label}
                </span>
              </li>
            ))}
          </ul>
          <p className="mt-4 border-t border-border pt-4 text-xs text-text-secondary">{t.ask.checklistNote}</p>
        </section>
      </aside>
    </div>
  );
}
