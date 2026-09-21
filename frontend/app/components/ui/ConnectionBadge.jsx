import { BadgeCheck } from "lucide-react";
import Badge from "./Badge";

/**
 * Shows a project's South African connection — the platform's own
 * curated classification (product definition §21/§22), not the
 * unpopulated Layer 2 `Project.verified` scoring field. That field stays
 * deliberately unsurfaced until Layer 2 defines real verification
 * criteria, so there is no separate "unverified" badge here.
 */
const LABELS = {
  south_african: "South African",
  community_verified: "Community Verified",
};

export default function ConnectionBadge({ connection }) {
  const label = LABELS[connection];
  if (!label) return null;

  return (
    <Badge tone="accent" icon={BadgeCheck} title={`Connection: ${label}`}>
      {label}
    </Badge>
  );
}
