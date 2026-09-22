import "./globals.css";
import { AuthProvider } from "./context/AuthContext";
import { ThemeProvider } from "./context/ThemeContext";
import SiteHeader from "./components/SiteHeader";
import Sidebar from "./components/Sidebar";

export const metadata = {
title: "Code Masters — African Open Source Discovery",
description:
"Discover African open-source projects, developers, and opportunities to contribute.",
};

export default function RootLayout({ children }) {
return (
    <html lang="en" suppressHydrationWarning>
      <body>
        <ThemeProvider>
          <AuthProvider>
            <div style={{ padding: "14px", minHeight: "100vh" }}>
              <SiteHeader />
              <div style={{ display: "flex", gap: "16px", alignItems: "flex-start" }}>
                <Sidebar />
                <main style={{ flex: 1, minWidth: 0 }}>{children}</main>
              </div>
            </div>
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}

