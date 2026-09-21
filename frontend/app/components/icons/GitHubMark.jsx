/**
 * The installed lucide-react version ships no brand/logo icons (GitHub's
 * mark isn't a generic glyph), so the one place the product needs to
 * visually identify GitHub specifically — auth and "contribute on GitHub"
 * actions — uses a small hand-authored mark instead. Every other icon in
 * the app comes from lucide-react (design brief §27: one icon family).
 */
export default function GitHubMark({ size = 16, className = "" }) {
  return (
    <svg
      viewBox="0 0 16 16"
      width={size}
      height={size}
      fill="currentColor"
      aria-hidden="true"
      className={className}
    >
      <path d="M8 0C3.58 0 0 3.64 0 8.13c0 3.6 2.29 6.65 5.47 7.72.4.08.55-.17.55-.39 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.5-2.69-.96-.09-.23-.48-.96-.82-1.16-.28-.15-.68-.52-.01-.53.63-.01 1.08.59 1.23.83.72 1.22 1.87.88 2.33.67.07-.53.28-.88.51-1.08-1.78-.2-3.64-.9-3.64-4.02 0-.89.31-1.62.83-2.19-.08-.2-.36-1.04.08-2.16 0 0 .67-.22 2.2.83a7.5 7.5 0 0 1 4 0c1.53-1.05 2.2-.83 2.2-.83.44 1.12.16 1.96.08 2.16.52.57.83 1.29.83 2.19 0 3.13-1.87 3.82-3.65 4.02.29.25.54.75.54 1.51 0 1.09-.01 1.97-.01 2.24 0 .22.15.48.55.39A8.13 8.13 0 0 0 16 8.13C16 3.64 12.42 0 8 0Z" />
    </svg>
  );
}
