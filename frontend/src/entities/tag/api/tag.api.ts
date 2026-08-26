import { httpClient } from "@/shared/api/http-client";
import type { Tag } from "../model/tag.types";

export const tagApi = {
  search: (q?: string) => {
    const query = q ? `?q=${encodeURIComponent(q)}` : "";
    return httpClient.get<Tag[]>(`/api/v1/tags${query}`);
  },
};
