export default function Checkbox({ label, className = "", ...rest }) {
  return (
    <label className={`flex cursor-pointer items-center gap-2.5 text-sm text-foreground-secondary ${className}`}>
      <input
        type="checkbox"
        className="h-4 w-4 rounded border-border-strong text-primary accent-primary focus-visible:outline-2 focus-visible:outline-primary"
        {...rest}
      />
      {label}
    </label>
  );
}
