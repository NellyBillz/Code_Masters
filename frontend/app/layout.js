import "./globals.css";

export const metadata = {
title: "Code Masters — African Open Source Discovery",
description:
"Discover African open-source projects, developers, and opportunities to contribute.",
};

export default function RootLayout({ children }) {
return ( <html lang="en"> <body>{children}</body> </html>
);
}

