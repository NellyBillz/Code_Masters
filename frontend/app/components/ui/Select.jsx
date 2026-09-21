import { ChevronDown } from "lucide-react";

export default function Select({ className = "", children, ...rest }) {
  return (
    <div className="relative">
      <select
        className={`h-10 w-full appearance-none rounded-md border border-border-strong bg-surface px-3 pr-9 text-sm text-foreground outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary-subtle disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
        {...rest}
      >
        {children}
      </select>
      <ChevronDown
        size={15}
        strokeWidth={1.75}
        aria-hidden="true"
        className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-foreground-muted"
      />
    </div>
  );
}
