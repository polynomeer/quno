export const organizationKeys = {
  all: ["organizations"] as const,
  search: (q: string) => [...organizationKeys.all, "search", q] as const,
  detail: (id: number) => [...organizationKeys.all, id] as const,
};
