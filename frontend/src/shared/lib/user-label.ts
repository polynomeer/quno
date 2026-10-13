/**
 * How a user is named in lists: their nickname when the API resolved one (ADR-0064), else the
 * old "사용자 #id" fallback — create/edit responses and live chat messages carry no nickname.
 */
export function userLabel(userId: number, nickname?: string | null): string {
  return nickname ?? `사용자 #${userId}`;
}
