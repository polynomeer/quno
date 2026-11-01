import { QunoBotIcon } from "./QunoBotIcon";

/** What readers other than the author see when QunoBot masked something (ADR-0081). */
export function MaskedSecretsNotice({ count }: { count: number }) {
  if (count <= 0) return null;
  return (
    <p className="inline-flex items-center gap-2 rounded-full bg-brand-subtle px-3 py-1 text-xs font-medium text-brand-on-subtle">
      <QunoBotIcon className="size-3.5" />
      QunoBot이 비밀값으로 보이는 문자열 {count}개를 가렸어요
    </p>
  );
}
