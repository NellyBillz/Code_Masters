import "./globals.css";
import { AuthProvider } from "./context/AuthContext";
import { ThemeProvider } from "./context/ThemeContext";
import AuthStatus from "./components/AuthStatus";
import ThemeToggle from "./components/ThemeToggle";

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
            <header
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "flex-end",
                gap: "0.75rem",
                padding: "1rem 1.5rem",
                background: "var(--cm-bg)",
                borderBottom: "0.5px solid var(--cm-border)",
              }}
            >
              <AuthStatus />
              <ThemeToggle />
            </header>
            {children}
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}

