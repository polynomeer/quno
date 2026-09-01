/** Mirrors backend TagResponse (interfaces/api/tag/TagResponses.kt). */
export interface Tag {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  docsUrl: string | null;
}

export type TagQuestionSort = "latest" | "unanswered" | "top";

/** Mirrors TagContributorResponse — ranked by how many answers they've posted to questions
 * carrying this tag (ADR-0040). */
export interface TagContributor {
  userId: number;
  nickname: string;
  answerCount: number;
}
