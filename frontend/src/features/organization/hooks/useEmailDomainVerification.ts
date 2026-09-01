"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { organizationApi } from "@/entities/organization/api/organization.api";
import { organizationKeys } from "@/entities/organization/api/organization.keys";
import { userKeys } from "@/entities/user/api/user.keys";

export function useRequestEmailDomainVerification() {
  return useMutation({
    mutationFn: (email: string) => organizationApi.requestEmailVerification(email),
  });
}

/** On success, the caller belongs to a (possibly newly created) Verified organization — refresh
 * their own profile so the new membership shows up (mirrors useToggleJoinOrganization). */
export function useConfirmEmailDomainVerification(viewerId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (code: string) => organizationApi.confirmEmailVerification(code),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: organizationKeys.all });
      if (viewerId) {
        queryClient.invalidateQueries({ queryKey: userKeys.profile(viewerId) });
      }
    },
  });
}
