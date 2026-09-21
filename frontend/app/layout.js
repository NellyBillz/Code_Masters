import { Geist, Geist_Mono, Bricolage_Grotesque } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "./context/AuthContext";
import Header from "./components/Header";
import { themeInitScript } from "./theme/theme-script";

const geistSans = Geist({
  subsets: ["latin"],
  variable: "--font-geist-sans",
});

const geistMono = Geist_Mono({
  subsets: ["latin"],
  variable: "--font-geist-mono",
});

// Display face for hero/section/page-title headlines only — see the
// "font-display" utility in globals.css. Everything else stays on Geist.
const bricolage = Bricolage_Grotesque({
  subsets: ["latin"],
  variable: "--font-display-src",
});

export const metadata = {
  title: "Code Masters — South African Open Source Discovery",
  description:
    "Discover South African open-source projects, understand where help is needed, and connect with the people building them.",
};

export default function RootLayout({ children }) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} ${bricolage.variable}`}
      suppressHydrationWarning
    >
      <head>
        {/* Applies the stored/system theme before paint — avoids a flash of the wrong theme. */}
        <script dangerouslySetInnerHTML={{ __html: themeInitScript() }} />
      </head>
      <body className="bg-background text-foreground" suppressHydrationWarning>
        <AuthProvider>
          <Header />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
