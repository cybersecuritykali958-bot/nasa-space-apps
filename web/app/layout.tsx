import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "NASA Space Apps Challenge",
  description: "Проект для хакатона NASA Space Apps Challenge",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ru" className="h-full antialiased">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
