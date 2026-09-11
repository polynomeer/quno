"use client";

import { Suspense, useState, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useSignUp } from "@/features/auth/hooks/useSignUp";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";
import { ApiError } from "@/shared/api/api-error";

function SignUpForm() {
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const signUp = useSignUp();
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirectTo = searchParams.get("redirectTo") || "/";

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await signUp.mutateAsync({ email, nickname, password });
      router.push(redirectTo);
    } catch {
      // error surfaced below via signUp.error
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mx-auto max-w-sm space-y-4 py-12">
      <h1 className="text-xl font-semibold">회원가입</h1>
      <Input
        type="email"
        placeholder="Email"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        autoComplete="email"
        required
      />
      <Input
        type="text"
        placeholder="Nickname"
        value={nickname}
        onChange={(event) => setNickname(event.target.value)}
        autoComplete="nickname"
        minLength={2}
        maxLength={50}
        required
      />
      <Input
        type="password"
        placeholder="Password"
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        autoComplete="new-password"
        minLength={8}
        maxLength={100}
        required
      />
      {signUp.isError && (
        <p className="text-sm text-danger">
          {signUp.error instanceof ApiError ? signUp.error.message : "회원가입에 실패했습니다."}
        </p>
      )}
      <Button type="submit" className="w-full" disabled={signUp.isPending}>
        {signUp.isPending ? "가입 중..." : "회원가입"}
      </Button>
      <p className="text-center text-sm text-text-secondary">
        이미 계정이 있으신가요?{" "}
        <Link href="/login" className="text-brand underline hover:no-underline">
          로그인
        </Link>
      </p>
    </form>
  );
}

export default function SignUpPage() {
  return (
    <Suspense>
      <SignUpForm />
    </Suspense>
  );
}
