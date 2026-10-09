import type { Metadata } from "next";
import { IBM_Plex_Sans_KR, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";
import { AppHeader } from "@/widgets/app-header/AppHeader";

// Body type needs Hangul coverage, which Geist lacks — see ADR-0061. next/font self-hosts every
// unicode-range slice Google serves; `subsets` only decides which slices get preloaded.
const plexKr = IBM_Plex_Sans_KR({
  variable: "--font-plex-kr",
  weight: ["400", "500", "600", "700"],
  subsets: ["latin"],
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-jetbrains-mono",
  subsets: ["latin"],
});

// Override via NEXT_PUBLIC_SITE_URL at deploy time — same pattern as QUNO_JWT_SECRET/QUNO_TOSS_*
// (see application.yml). Without it, relative OG/canonical URLs would resolve to localhost.
const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
const SITE_TITLE = "Quno";
const SITE_DESCRIPTION = "개발자를 위한 살아있는 Q&A 플랫폼";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: { default: SITE_TITLE, template: `%s - ${SITE_TITLE}` },
  description: SITE_DESCRIPTION,
  openGraph: {
    title: SITE_TITLE,
    description: SITE_DESCRIPTION,
    siteName: SITE_TITLE,
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary",
    title: SITE_TITLE,
    description: SITE_DESCRIPTION,
  },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className={`${plexKr.variable} ${jetbrainsMono.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col bg-canvas text-text-primary">
        <Providers>
          <AppHeader />
          <main className="mx-auto w-full max-w-[1360px] flex-1 px-4 py-8 sm:px-6">{children}</main>
        </Providers>
      </body>
    </html>
  );
}
