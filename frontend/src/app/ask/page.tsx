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

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_280px]">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <h1 className="text-xl font-semibold">{t.ask.heading}</h1>

        <div>
          <Input placeholder="Title" {...register("title")} />
          {errors.title && <p className="mt-1 text-sm text-danger">{errors.title.message}</p>}
        </div>

        <div>
          <Controller
            control={control}
            name="body"
            render={({ field }) => (
              <MarkdownEditor value={field.value} onChange={field.onChange} rows={12} placeholder={t.ask.bodyPlaceholder} />
            )}
          />
          {errors.body && <p className="mt-1 text-sm text-danger">{errors.body.message}</p>}
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-text-secondary">{t.ask.environmentLabel}</label>
          <Textarea rows={2} placeholder={t.ask.environmentPlaceholder} {...register("environment")} />
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-text-secondary">{t.ask.logsLabel}</label>
          <Textarea rows={4} placeholder={t.ask.logsPlaceholder} {...register("logs")} />
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-text-secondary">{t.ask.tagsLabel}</label>
          <TagInput value={tags} onChange={setTags} />
        </div>

        <FormError error={createQuestion.error} fallback={t.ask.failed} />

        <Button type="submit" disabled={createQuestion.isPending}>
          {createQuestion.isPending ? t.ask.submitting : t.ask.submit}
        </Button>
      </form>

      <aside className="space-y-3">
        <h2 className="text-sm font-semibold text-text-secondary">{t.ask.similarQuestions}</h2>
        {similarQuery ? (
          <QuestionList questions={similarQuestions ?? []} emptyMessage={t.ask.noSimilarQuestions} />
        ) : (
          <p className="text-sm text-text-secondary">{t.ask.similarQuestionsHint}</p>
        )}
      </aside>
    </div>
  );
}
