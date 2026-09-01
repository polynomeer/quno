"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { organizationApi } from "@/entities/organization/api/organization.api";
import { organizationKeys } from "@/entities/organization/api/organization.keys";
import { userKeys } from "@/entities/user/api/user.keys";

export function useToggleJoinOrganization(organizationId: number, viewerId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (isMember: boolean) =>
      isMember ? organizationApi.leave(organizationId) : organizationApi.join(organizationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: organizationKeys.detail(organizationId) });
      if (viewerId) {
        queryClient.invalidateQueries({ queryKey: userKeys.profile(viewerId) });
      }
    },
  });
}
