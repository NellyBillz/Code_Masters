import { MapPin, ArrowUpRight } from "lucide-react";
import { getPublicProfile } from "../../../lib/api";
import GitHubStats from "../../components/GitHubStats";
import StatTile from "../../components/StatTile";
import RecentContributions from "../../components/RecentContributions";

export default async function PublicProfilePage({ params }) {
  const { username } = await params;

  let user;
  try {
    user = await getPublicProfile(username);
  } catch (err) {
    return (
      <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
        <div className="cm-glass" style={{ borderRadius: "24px", padding: "48px 24px", textAlign: "center" }}>
          <p style={{ fontSize: "15px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>
            {err.status === 404 ? "This developer isn't on Code Masters" : "We couldn't load this profile"}
          </p>
          <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: 0 }}>
            {err.status === 404
              ? `No user exists with username "${username}".`
              : err.message || "The platform may be temporarily unavailable."}
          </p>
        </div>
      </div>
    );
  }

  const initials = (user.displayName || user.username || "?")[0]?.toUpperCase();
  const skills = user.skills || [];

  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
      {/* Header */}
      <header className="cm-glass" style={{ borderRadius: "28px", padding: "28px", marginBottom: "20px" }}>
        <div style={{ display: "flex", alignItems: "flex-start", gap: "18px", flexWrap: "wrap" }}>
          {user.avatarUrl ? (
            // eslint-disable-next-line @next/next/no-img-element -- external, size-variable avatar URL
            <img
              src={user.avatarUrl}
              alt=""
              width={72}
              height={72}
              style={{ borderRadius: "50%", flexShrink: 0 }}
            />
          ) : (
            <span
              aria-hidden="true"
              style={{
                width: "72px",
                height: "72px",
                borderRadius: "50%",
                background: "var(--cm-sidebar)",
                color: "var(--cm-lime)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 700,
                fontSize: "26px",
                flexShrink: 0,
              }}
            >
              {initials}
            </span>
          )}

          <div style={{ minWidth: 0, flex: 1 }}>
            <h1 style={{ fontSize: "22px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
              {user.displayName || user.username}
            </h1>

            <a
              href={`https://github.com/${user.username}`}
              target="_blank"
              rel="noreferrer"
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "4px",
                fontSize: "13px",
                fontWeight: 600,
                color: "var(--cm-text-secondary)",
                margin: "4px 0 0",
              }}
            >
              @{user.username}
              <ArrowUpRight size={13} strokeWidth={2} aria-hidden="true" />
            </a>

            <p style={{ fontSize: "13px", lineHeight: 1.6, color: user.bio ? "var(--cm-text-secondary)" : "var(--cm-text-muted)", margin: "10px 0 0", maxWidth: "520px", fontStyle: user.bio ? "normal" : "italic" }}>
              {user.bio || "This developer hasn't added a bio yet."}
            </p>

            {user.location && (
              <p style={{ display: "flex", alignItems: "center", gap: "5px", fontSize: "12.5px", color: "var(--cm-text-muted)", margin: "10px 0 0" }}>
                <MapPin size={13} strokeWidth={1.8} aria-hidden="true" />
                {user.location}
              </p>
            )}

            <GitHubStats username={user.username} />
          </div>
        </div>

        {skills.length > 0 && (
          <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "18px" }}>
            {skills.map((skill) => (
              <span
                key={skill}
                style={{
                  fontSize: "11.5px",
                  fontWeight: 600,
                  color: "var(--cm-text-secondary)",
                  background: "var(--cm-surface-alt)",
                  padding: "4px 11px",
                  borderRadius: "999px",
                }}
              >
                {skill}
              </span>
            ))}
          </div>
        )}
      </header>

      {/* Stats */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "12px", marginBottom: "20px" }}>
        <StatTile label="Verified contributions" value={user.contributionsCount ?? 0} />
        <StatTile label="Projects" value={user.projectsCount ?? "—"} />
        <StatTile label="Reputation" value={user.reputation ?? 0} />
      </div>

      <RecentContributions username={user.username} />
    </div>
  );
}
