"use client";

import { Suspense, useState, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useLogin } from "@/features/auth/hooks/useLogin";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";
import { FormError } from "@/shared/ui/FormError";
import { useLocale } from "@/shared/i18n/LocaleProvider";

function LoginForm() {
  const { t } = useLocale();
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
      <h1 className="text-xl font-semibold">{t.login.title}</h1>
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
      <FormError error={login.error} fallback={t.login.failed} />
      <Button type="submit" className="w-full" disabled={login.isPending}>
        {login.isPending ? t.login.submitting : t.login.submit}
      </Button>
      <p className="text-center text-sm text-text-secondary">
        {t.login.noAccount}{" "}
        <Link href="/signup" className="text-brand underline hover:no-underline">
          {t.login.signUpLink}
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
