"use client";

import { useState } from "react";
import { useUpdateTagDetails } from "../hooks/useUpdateTagDetails";
import { Textarea } from "@/shared/ui/Textarea";
import { Input } from "@/shared/ui/Input";
import { Button } from "@/shared/ui/Button";
import { ApiError } from "@/shared/api/api-error";
import type { Tag } from "@/entities/tag/model/tag.types";

/** Wiki-style — any logged-in user can edit (ADR-0040), same trust level as CreateOrganizationForm
 * and Tag creation itself (implicit, via tagging a question). */
export function TagDetailsEditor({ tag }: { tag: Tag }) {
  const [editing, setEditing] = useState(false);
  const [description, setDescription] = useState(tag.description ?? "");
  const [docsUrl, setDocsUrl] = useState(tag.docsUrl ?? "");
  const updateDetails = useUpdateTagDetails(tag.id);

  async function handleSave() {
    try {
      await updateDetails.mutateAsync({ description: description.trim(), docsUrl: docsUrl.trim() });
      setEditing(false);
    } catch {
      // error surfaced below via updateDetails.error
    }
  }

  if (!editing) {
    return (
      <div className="space-y-2">
        {tag.description && <p className="text-sm text-text-primary">{tag.description}</p>}
        {tag.docsUrl && (
          <a href={tag.docsUrl} target="_blank" rel="noreferrer" className="block text-sm text-brand hover:underline">
            공식 문서 →
          </a>
        )}
        {!tag.description && !tag.docsUrl && <p className="text-sm text-text-secondary">아직 설명이 없습니다.</p>}
        <Button variant="ghost" className="px-0 text-xs" onClick={() => setEditing(true)}>
          편집
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <Textarea
        value={description}
        onChange={(event) => setDescription(event.target.value)}
        rows={3}
        placeholder="이 태그에 대한 설명"
        maxLength={2000}
      />
      <Input
        value={docsUrl}
        onChange={(event) => setDocsUrl(event.target.value)}
        placeholder="공식 문서 URL (선택)"
        maxLength={500}
      />
      {updateDetails.isError && (
        <p className="text-sm text-danger">
          {updateDetails.error instanceof ApiError ? updateDetails.error.message : "저장하지 못했습니다."}
        </p>
      )}
      <div className="flex gap-2">
        <Button onClick={handleSave} disabled={updateDetails.isPending}>
          {updateDetails.isPending ? "저장 중..." : "저장"}
        </Button>
        <Button
          variant="ghost"
          onClick={() => {
            setDescription(tag.description ?? "");
            setDocsUrl(tag.docsUrl ?? "");
            setEditing(false);
          }}
        >
          취소
        </Button>
      </div>
    </div>
  );
}
