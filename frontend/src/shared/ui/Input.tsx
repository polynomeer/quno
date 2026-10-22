import { type InputHTMLAttributes, forwardRef } from "react";
import { cn } from "@/shared/lib/cn";

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(function Input(
  { className, ...props },
  ref,
) {
  return (
    <input
      ref={ref}
      className={cn(
        "w-full rounded-2xl border border-border-strong bg-surface px-4 py-2.5 text-[15px] text-text-primary placeholder:text-text-secondary focus:border-brand focus:outline-none focus:ring-4 focus:ring-brand/15",
        className,
      )}
      {...props}
    />
  );
});
