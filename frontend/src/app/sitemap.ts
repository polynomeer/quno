import type { MetadataRoute } from "next";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";
const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

/** Google's own sitemap docs treat "give it a number no one will hit" as normal practice for an
 * unbounded list — this isn't a real pagination limit, just enough to not silently drop entries
 * for the sizes this app realistically has today (Phase 31, ADR-0043). */
const ENUMERATION_LIMIT = 1000;

async function fetchAllNames(): Promise<string[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/tags?limit=${ENUMERATION_LIMIT}`, { cache: "no-store" });
    if (!response.ok) return [];
    const tags = (await response.json()) as { name: string }[];
    return tags.map((tag) => tag.name);
  } catch {
    return [];
  }
}

async function fetchAllOrganizationIds(): Promise<number[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/organizations?limit=${ENUMERATION_LIMIT}`, { cache: "no-store" });
    if (!response.ok) return [];
    const organizations = (await response.json()) as { id: number }[];
    return organizations.map((organization) => organization.id);
  } catch {
    return [];
  }
}

/**
 * Static routes + every tag/organization detail page (Phase 31, ADR-0043). Deliberately excludes
 * question and user-profile URLs — there's no "list everything" endpoint for either (search
 * requires a query, profiles have no listing at all), and adding one just for the sitemap was
 * judged out of scope. A failed fetch degrades to an empty list for that section rather than
 * failing the whole sitemap.
 */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const [tagNames, organizationIds] = await Promise.all([fetchAllNames(), fetchAllOrganizationIds()]);

  const staticRoutes: MetadataRoute.Sitemap = [
    { url: SITE_URL, changeFrequency: "daily", priority: 1 },
    { url: `${SITE_URL}/tags`, changeFrequency: "weekly", priority: 0.6 },
    { url: `${SITE_URL}/organizations`, changeFrequency: "weekly", priority: 0.5 },
  ];

  const tagRoutes: MetadataRoute.Sitemap = tagNames.map((name) => ({
    url: `${SITE_URL}/tags/${encodeURIComponent(name)}`,
    changeFrequency: "daily",
    priority: 0.7,
  }));

  const organizationRoutes: MetadataRoute.Sitemap = organizationIds.map((id) => ({
    url: `${SITE_URL}/organizations/${id}`,
    changeFrequency: "weekly",
    priority: 0.4,
  }));

  return [...staticRoutes, ...tagRoutes, ...organizationRoutes];
}
