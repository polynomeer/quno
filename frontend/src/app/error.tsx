"use client";

import { useEffect } from "react";
import { Button } from "@/shared/ui/Button";
import { reportError } from "@/shared/lib/errorReporting";

export default function Error({
  error,
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  useEffect(() => {
    reportError(error);
  }, [error]);

  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-4 px-4 py-24 text-center">
      <h2 className="text-lg font-semibold text-text-primary">문제가 발생했습니다</h2>
      <p className="text-sm text-text-primary/70">
        페이지를 불러오는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.
      </p>
      <Button onClick={() => retry()}>다시 시도</Button>
    </div>
  );
}
