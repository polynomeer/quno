export type VoteTargetType = "QUESTION" | "ANSWER";

export interface MyVote {
  targetType: VoteTargetType;
  targetId: number;
  value: number;
}
