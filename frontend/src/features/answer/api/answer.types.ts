export interface Answer {
  id: number;
  questionId: number;
  authorId: number;
  body: string;
  isAccepted: boolean;
  targetVersionNumber: number;
  isStale: boolean;
  createdAt: string;
  updatedAt: string;
}
