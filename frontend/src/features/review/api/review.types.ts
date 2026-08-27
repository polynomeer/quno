export type ReviewRequestStatus = "OPEN" | "ADDRESSED";

export interface ReviewRequest {
  id: number;
  questionId: number;
  requestedBy: number;
  message: string;
  status: ReviewRequestStatus;
  questionVersionNumberAtRequest: number;
  createdAt: string;
  addressedAt: string | null;
}
