import clsx, { type ClassValue } from "clsx";

/** Combines conditional class names — see docs/frontend/architecture.md #26 디렉터리·코드 구조. */
export function cn(...inputs: ClassValue[]): string {
  return clsx(inputs);
}
