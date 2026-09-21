import "./globals.css";
import { AuthProvider } from "./context/AuthContext";
import AuthStatus from "./components/AuthStatus";

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
              padding: "1rem 1.5rem",
              borderBottom: "1px solid #eee",
            }}
          >
            <AuthStatus />
          </header>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}

