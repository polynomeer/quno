/**
 * Output caps for MCP tools (Phase 65.4, ADR-0080): an agent's context is the scarce resource,
 * so long bodies are cut per field and the whole payload is bounded too.
 */
export const MAX_FIELD_CHARS = 4_000;
export const MAX_RESULT_CHARS = 20_000;

export function clip(text: string | null | undefined, max = MAX_FIELD_CHARS): { text: string | null; truncated: boolean } {
  if (text == null) return { text: null, truncated: false };
  return text.length > max ? { text: text.slice(0, max) + "…", truncated: true } : { text, truncated: false };
}

/**
 * Serialises [payload]; when it is still over [max], drops trailing entries of [listKey] one by one
 * (least relevant last) and marks the result truncated.
 */
export function boundedJson<T extends Record<string, unknown>>(payload: T, listKey: keyof T, max = MAX_RESULT_CHARS): { json: string; value: T } {
  let value = payload;
  let json = JSON.stringify(value, null, 2);
  while (json.length > max) {
    const list = value[listKey];
    if (!Array.isArray(list) || list.length === 0) break;
    value = { ...value, [listKey]: list.slice(0, -1), truncated: true };
    json = JSON.stringify(value, null, 2);
  }
  return { json, value };
}
