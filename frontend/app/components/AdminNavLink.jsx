"use client";

import Link from "next/link";
import { useAuth } from "../context/AuthContext";

export default function AdminNavLink() {
    const { user } = useAuth();

    if (!user?.isSiteAdmin) {
        return null;
    }

    return (
        <Link href="/admin/projects" style={{ fontSize: "0.9rem" }}>
            Moderation queue
        </Link>
    );
}
