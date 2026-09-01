import type { Metadata } from "next";
import { QuestionDetailContent } from "./QuestionDetailContent";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";

interface QuestionMetadataSource {
  title: string;
  body: string;
}

/** Plain unauthenticated fetch against the now-public `GET /questions/{id}` (Phase 29,
 * ADR-0041) — deliberately not `httpClient` (that reads `localStorage`, which doesn't exist in
 * this Server Component). Never throws: a failed/404 fetch just means no dynamic metadata,
 * not a broken page (Phase 31, ADR-0043). */
async function fetchQuestionForMetadata(id: string): Promise<QuestionMetadataSource | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/questions/${id}`, { cache: "no-store" });
    if (!response.ok) return null;
    return (await response.json()) as QuestionMetadataSource;
  } catch {
    return null;
  }
}

function excerpt(body: string, maxLength = 200): string {
  const oneLine = body.replace(/\s+/g, " ").trim();
  return oneLine.length > maxLength ? `${oneLine.slice(0, maxLength)}…` : oneLine;
}

export async function generateMetadata({ params }: PageProps<"/questions/[id]">): Promise<Metadata> {
  const { id } = await params;
  const question = await fetchQuestionForMetadata(id);

  if (!question) {
    return { title: "질문을 찾을 수 없습니다" };
  }

  const description = excerpt(question.body);
  return {
    title: question.title,
    description,
    openGraph: {
      title: question.title,
      description,
      type: "article",
    },
    twitter: {
      card: "summary",
      title: question.title,
      description,
    },
  };
}

export default async function QuestionDetailPage({ params }: PageProps<"/questions/[id]">) {
  const { id } = await params;
  return <QuestionDetailContent questionId={Number(id)} />;
}
