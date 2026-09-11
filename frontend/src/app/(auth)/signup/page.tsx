"use client";

import { Suspense, useState, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useSignUp } from "@/features/auth/hooks/useSignUp";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";
import { FormError } from "@/shared/ui/FormError";
import { useLocale } from "@/shared/i18n/LocaleProvider";

function SignUpForm() {
  const { t } = useLocale();
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
      <h1 className="text-xl font-semibold">{t.signup.title}</h1>
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
      <FormError error={signUp.error} fallback={t.signup.failed} />
      <Button type="submit" className="w-full" disabled={signUp.isPending}>
        {signUp.isPending ? t.signup.submitting : t.signup.submit}
      </Button>
      <p className="text-center text-sm text-text-secondary">
        {t.signup.haveAccount}{" "}
        <Link href="/login" className="text-brand underline hover:no-underline">
          {t.signup.loginLink}
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
