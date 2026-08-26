"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useLogin } from "@/features/auth/hooks/useLogin";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";
import { ApiError } from "@/shared/api/api-error";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const login = useLogin();
  const router = useRouter();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await login.mutateAsync({ email, password });
      router.push("/");
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
      {login.isError && (
        <p className="text-sm text-danger">
          {login.error instanceof ApiError ? login.error.message : "로그인에 실패했습니다."}
        </p>
      )}
      <Button type="submit" className="w-full" disabled={login.isPending}>
        {login.isPending ? "로그인 중..." : "로그인"}
      </Button>
    </form>
  );
}
