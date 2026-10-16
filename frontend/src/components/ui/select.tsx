import { forwardRef, type SelectHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  invalid?: boolean;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(({ className, invalid, children, ...props }, ref) => (
  <select
    ref={ref}
    aria-invalid={invalid || undefined}
    className={cn(
      "h-10 w-full rounded-lg border border-border bg-surface px-3 text-sm text-foreground",
      "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-60",
      invalid && "border-danger",
      className,
    )}
    {...props}
  >
    {children}
  </select>
));
Select.displayName = "Select";
