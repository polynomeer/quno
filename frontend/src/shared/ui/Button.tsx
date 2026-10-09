import { type ButtonHTMLAttributes, forwardRef } from "react";
import { cn } from "@/shared/lib/cn";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary: "bg-brand text-brand-foreground font-semibold hover:opacity-90",
  // Outlined ink — the design's "질문하기" weight: present everywhere, but never louder than search.
  secondary: "bg-surface text-text-primary font-semibold border border-text-primary hover:bg-surface-subtle",
  ghost: "text-text-body hover:bg-surface-subtle hover:text-text-primary",
  // Subtle fill instead of solid red + white text, which failed contrast in dark mode.
  danger: "bg-danger-subtle text-danger font-semibold border border-danger/30 hover:border-danger/60",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { className, variant = "primary", ...props },
  ref,
) {
  return (
    <button
      ref={ref}
      className={cn(
        "inline-flex min-h-10 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:pointer-events-none disabled:opacity-50",
        variantClasses[variant],
        className,
      )}
      {...props}
    />
  );
});
