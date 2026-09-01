"use client";

import { useQuery } from "@tanstack/react-query";
import { organizationApi } from "../api/organization.api";
import { organizationKeys } from "../api/organization.keys";

export function useOrganizationSearch(q: string) {
  return useQuery({
    queryKey: organizationKeys.search(q),
    queryFn: () => organizationApi.search(q || undefined),
  });
}
