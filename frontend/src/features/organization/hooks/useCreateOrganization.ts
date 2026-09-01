"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { organizationApi } from "@/entities/organization/api/organization.api";
import { organizationKeys } from "@/entities/organization/api/organization.keys";

export function useCreateOrganization() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: { name: string; description?: string }) => organizationApi.create(input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: organizationKeys.all });
    },
  });
}
