"use client";

import { Suspense, useState, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useLogin } from "@/features/auth/hooks/useLogin";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";
import { FormError } from "@/shared/ui/FormError";

function LoginForm() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const login = useLogin();
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirectTo = searchParams.get("redirectTo") || "/";

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await login.mutateAsync({ email, password });
      router.push(redirectTo);
    } catch {
      // error surfaced below via login.error
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mx-auto max-w-sm space-y-4 py-12">
      <h1 className="text-xl font-semibold">로그인</h1>
      <Input
        type="email"
        placeholder="Email"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        autoComplete="email"
        required
      />
      <Input
        type="password"
        placeholder="Password"
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        autoComplete="current-password"
        required
      />
      <FormError error={login.error} fallback="로그인에 실패했습니다." />
      <Button type="submit" className="w-full" disabled={login.isPending}>
        {login.isPending ? "로그인 중..." : "로그인"}
      </Button>
      <p className="text-center text-sm text-text-secondary">
        계정이 없으신가요?{" "}
        <Link href="/signup" className="text-brand underline hover:no-underline">
          회원가입
        </Link>
      </p>
    </form>
  );
}

export default function LoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}
