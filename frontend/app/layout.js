import "./globals.css";
import { AuthProvider } from "./context/AuthContext";
import AuthStatus from "./components/AuthStatus";
import AdminNavLink from "./components/AdminNavLink";

export const metadata = {
title: "Code Masters — African Open Source Discovery",
description:
"Discover African open-source projects, developers, and opportunities to contribute.",
};

export default function RootLayout({ children }) {
return (
    <html lang="en">
      <body>
        <AuthProvider>
          <header
            style={{
              display: "flex",
              justifyContent: "flex-end",
              alignItems: "center",
              gap: "1.25rem",
              padding: "1rem 1.5rem",
              borderBottom: "1px solid #eee",
            }}
          >
            <AdminNavLink />
            <AuthStatus />
          </header>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}

