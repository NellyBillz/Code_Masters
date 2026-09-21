"use client";

import Link from "next/link";
import { useAuth } from "../context/AuthContext";

export default function MaintainerActivityNavLink() {
    const { user } = useAuth();

    if (!user) {
        return null;
    }

    return (
        <Link href="/maintainer-activity" style={{ fontSize: "0.9rem" }}>
            Maintainer activity
        </Link>
    );
}
