/**
 * Shared text input primitive. Used for search fields and short-form text
 * entry (design brief §34's "Input"). Textareas use the same visual
 * treatment directly where needed (e.g. IssueComments) rather than a
 * separate component, since there's exactly one such field in the app.
 */
export default function Input({ className = "", ...rest }) {
  return (
    <input
      className={`h-10 w-full rounded-md border border-border-strong bg-surface px-3 text-sm text-foreground outline-none transition-colors placeholder:text-foreground-disabled focus:border-primary focus:ring-2 focus:ring-primary-subtle disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
      {...rest}
    />
  );
}
