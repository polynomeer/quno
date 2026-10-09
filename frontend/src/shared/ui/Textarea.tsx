import { type TextareaHTMLAttributes, forwardRef } from "react";
import { cn } from "@/shared/lib/cn";

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  function Textarea({ className, ...props }, ref) {
    return (
      <textarea
        ref={ref}
        className={cn(
          "w-full rounded-lg border border-border-strong bg-surface px-3.5 py-2.5 text-[15px] text-text-primary placeholder:text-text-secondary focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand",
          className,
        )}
        {...props}
      />
    );
  },
);
