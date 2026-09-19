import "./globals.css";
import { AuthProvider } from './context/AuthContext';

export const metadata = {
  title: "Code Masters — African Open Source Discovery",
  description: "Discover African open-source projects, developers, and opportunities to contribute.",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
