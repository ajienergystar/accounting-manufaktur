import type { Metadata } from "next";
import "./globals.css";
import { Providers } from "@/components/providers";
import { themeInitScript } from "@/lib/theme";

export const metadata: Metadata = {
  title: {
    default: "Akuntansi Manufaktur",
    template: "%s · Akuntansi Manufaktur",
  },
  description: "Aplikasi pembukuan akuntansi untuk perusahaan manufaktur",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="id" suppressHydrationWarning className="h-full antialiased">
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body className="min-h-full">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
