/** Same origin as NEXT_PUBLIC_API_BASE_URL, just ws(s):// instead of http(s):// — the backend's
 * STOMP endpoint (WebSocketConfig) lives on the same Spring Boot server as the REST API. */
export function getLiveChatWebSocketUrl(): string {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";
  return apiBase.replace(/^http/, "ws") + "/ws";
}
