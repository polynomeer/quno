import { httpClient } from "@/shared/api/http-client";
import type { MyVote, VoteTargetType } from "./vote.types";

function votePath(targetType: VoteTargetType, targetId: number): string {
  return targetType === "QUESTION" ? `/api/v1/questions/${targetId}/vote` : `/api/v1/answers/${targetId}/vote`;
}

export const voteApi = {
  myVotes: () => httpClient.get<MyVote[]>("/api/v1/me/votes"),
  cast: (targetType: VoteTargetType, targetId: number, value: 1 | -1) =>
    httpClient.post<void>(votePath(targetType, targetId), { value }),
  retract: (targetType: VoteTargetType, targetId: number) => httpClient.delete<void>(votePath(targetType, targetId)),
};
