/** `relevance` is the pre-existing default (backend: actually `id DESC`, not a true full-text
 * ranking — see api-design.md). `score` orders by net vote score (Phase 20, ADR-0032). */
export type SearchSort = "relevance" | "score";
