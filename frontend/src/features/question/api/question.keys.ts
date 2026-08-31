export const questionKeys = {
  all: ["questions"] as const,
  detail: (id: number) => [...questionKeys.all, id, "detail"] as const,
  versions: (id: number) => [...questionKeys.all, id, "versions"] as const,
  diff: (id: number, version: number, from?: number) =>
    [...questionKeys.all, id, "versions", version, "diff", from ?? "default"] as const,
  related: (id: number) => [...questionKeys.all, id, "related"] as const,
  graph: (id: number) => [...questionKeys.all, id, "graph"] as const,
};
