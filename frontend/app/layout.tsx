import type { Metadata } from "next";
import "./globals.css";
import Navbar from "@/components/Navbar";

export const metadata: Metadata = {
  title: "AI Interview Coach",
  description: "面向 Java 后端求职者的 AI 面试训练系统",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body className="bg-background text-on-surface min-h-screen font-sans flex flex-col">
        <Navbar />
        {children}
      </body>
    </html>
  );
}
