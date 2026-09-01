"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/features/auth/hooks/useSession";
import { useCreateOrganization } from "../hooks/useCreateOrganization";
import { Input } from "@/shared/ui/Input";
import { Textarea } from "@/shared/ui/Textarea";
import { Button } from "@/shared/ui/Button";
import { ApiError } from "@/shared/api/api-error";

/** Same trust level as creating a Tag (ADR-0034) — no approval step, just a name collision
 * check the backend enforces (DuplicateOrganizationNameException, surfaced via createOrganization.error).
 * Organizations list is publicly readable now (Phase 30, ADR-0042) — hides for an anonymous
 * visitor rather than surfacing a form that 401s on submit. */
export function CreateOrganizationForm() {
  const { data: me } = useSession();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const createOrganization = useCreateOrganization();

  if (!me) {
    return null;
  }

  if (!open) {
    return (
      <Button variant="secondary" onClick={() => setOpen(true)}>
        새 조직 만들기
      </Button>
    );
  }

  async function handleSubmit() {
    if (!name.trim()) return;
    try {
      const organization = await createOrganization.mutateAsync({
        name: name.trim(),
        description: description.trim() || undefined,
      });
      router.push(`/organizations/${organization.id}`);
    } catch {
      // error surfaced below via createOrganization.error
    }
  }

  return (
    <div className="space-y-2 rounded-lg border border-border p-4">
      <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="조직 이름" maxLength={100} />
      <Textarea
        value={description}
        onChange={(event) => setDescription(event.target.value)}
        rows={2}
        placeholder="설명 (선택)"
        maxLength={2000}
      />
      {createOrganization.isError && (
        <p className="text-sm text-danger">
          {createOrganization.error instanceof ApiError ? createOrganization.error.message : "조직을 만들지 못했습니다."}
        </p>
      )}
      <div className="flex gap-2">
        <Button onClick={handleSubmit} disabled={createOrganization.isPending || !name.trim()}>
          {createOrganization.isPending ? "만드는 중..." : "만들기"}
        </Button>
        <Button variant="ghost" onClick={() => setOpen(false)}>
          취소
        </Button>
      </div>
    </div>
  );
}
