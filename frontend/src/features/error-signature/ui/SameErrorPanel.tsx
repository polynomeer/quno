"use client";

import Link from "next/link";
import { useErrorSignaturePreview } from "../hooks/useErrorSignaturePreview";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { useLocale } from "@/shared/i18n/LocaleProvider";

/**
 * Shown under the ask form's log field once a pasted trace yields an error signature (Phase 64,
 * ADR-0075; the "같은 에러 N건" panel of web design v2). Renders nothing for text without an
 * exception, so it never nags people who aren't asking about an error.
 */
export function SameErrorPanel({ text }: { text: string }) {
  const { t } = useLocale();
  const { data } = useErrorSignaturePreview(text);

  if (!data || data.signatures.length === 0) return null;

  const count = data.questions.length;
  return (
    <section aria-label={t.ask.sameErrorLabel} className="space-y-3 rounded-[20px] bg-brand-subtle p-4">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5 px-1">
        <h2 className="text-sm font-bold text-brand-on-subtle">
          {count > 0 ? t.ask.sameErrorHeading.replace("{count}", String(count)) : t.ask.sameErrorLabel}
        </h2>
        <span className="text-xs text-text-body">{t.ask.sameErrorSignature}</span>
        {data.signatures.map((signature) => (
          <code
            key={signature.messageKey + (signature.signatureHash ?? "")}
            className="rounded-full bg-surface px-2.5 py-0.5 font-mono text-xs text-text-primary"
          >
            {signature.exceptionClass.split(".").pop()}
          </code>
        ))}
      </div>
      {count === 0 ? (
        <p className="px-1 text-sm text-text-body">{t.ask.sameErrorNone}</p>
      ) : (
        <ul className="space-y-2">
          {data.questions.map((question) => (
            <li key={question.id}>
              <Link
                href={`/questions/${question.id}`}
                className="flex flex-wrap items-center gap-x-2.5 gap-y-1 rounded-2xl bg-surface px-3.5 py-3 hover:opacity-90"
              >
                <StatusBadge status={question.status} />
                <span className="min-w-0 flex-1 text-sm font-semibold text-text-primary">{question.title}</span>
                {question.hasAcceptedAnswer && (
                  <span className="text-xs font-semibold text-success">{t.ask.sameErrorAccepted}</span>
                )}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
