import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { cn } from "@/shared/lib/cn";

/**
 * react-markdown renders the AST straight to React elements (no dangerouslySetInnerHTML),
 * so it's safe against injected HTML by default — see docs/frontend/architecture.md #31.
 */
export function MarkdownContent({ children, className }: { children: string; className?: string }) {
  return (
    <div className={cn("markdown-body", className)}>
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{children}</ReactMarkdown>
    </div>
  );
}
