export default function StatTile({ label, value }) {
  return (
    <div className="cm-glass" style={{ borderRadius: "20px", padding: "18px", textAlign: "center" }}>
      <p style={{ fontSize: "22px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>{value}</p>
      <p style={{ fontSize: "12px", color: "var(--cm-text-secondary)", margin: "4px 0 0" }}>{label}</p>
    </div>
  );
}
