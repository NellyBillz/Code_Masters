import Badge from "./Badge";

const LABELS = {
  beginner: "Beginner",
  intermediate: "Intermediate",
  advanced: "Advanced",
  unknown: "Unclassified",
};

const TONES = {
  beginner: "success",
  intermediate: "primary",
  advanced: "danger",
  unknown: "neutral",
};

export default function DifficultyBadge({ difficulty }) {
  const key = (difficulty || "unknown").toLowerCase();
  return (
    <Badge tone={TONES[key] || "neutral"}>{LABELS[key] || "Unclassified"}</Badge>
  );
}
