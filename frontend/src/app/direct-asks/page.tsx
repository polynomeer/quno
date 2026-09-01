"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useMyDirectAsks } from "@/features/direct-ask/hooks/useMyDirectAsks";
import { DirectAskRequestList } from "@/features/direct-ask/ui/DirectAskRequestList";
import { Button } from "@/shared/ui/Button";
import { Skeleton } from "@/shared/ui/Skeleton";

function DirectAsksContent() {
  const { isLoading: authLoading } = useRequireAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const role = searchParams.get("role") === "sent" ? "sent" : "received";
  const { data: items, isLoading } = useMyDirectAsks(role, !authLoading);

  if (authLoading || isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Direct Asks</h1>
      <div className="flex gap-2">
        <Button variant={role === "received" ? "primary" : "secondary"} onClick={() => router.push("/direct-asks?role=received")}>
          받은 요청
        </Button>
        <Button variant={role === "sent" ? "primary" : "secondary"} onClick={() => router.push("/direct-asks?role=sent")}>
          보낸 요청
        </Button>
      </div>
      <DirectAskRequestList
        items={items ?? []}
        role={role}
        emptyMessage={role === "received" ? "받은 Direct Ask 요청이 없습니다." : "보낸 Direct Ask 요청이 없습니다."}
      />
    </div>
  );
}

export default function DirectAsksPage() {
  return (
    <Suspense>
      <DirectAsksContent />
    </Suspense>
  );
}
