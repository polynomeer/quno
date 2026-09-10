import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-4 px-4 py-24 text-center">
      <h2 className="text-lg font-semibold text-text-primary">페이지를 찾을 수 없습니다</h2>
      <p className="text-sm text-text-primary/70">요청하신 페이지가 존재하지 않거나 삭제되었습니다.</p>
      <Link
        href="/"
        className="inline-flex items-center justify-center gap-2 rounded-md bg-brand px-4 py-2 text-sm font-medium text-brand-foreground transition-colors hover:opacity-90"
      >
        홈으로
      </Link>
    </div>
  );
}
