import { forwardRef, type InputHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(({ className, invalid, ...props }, ref) => (
  <input
    ref={ref}
    aria-invalid={invalid || undefined}
    className={cn(
      "h-10 w-full rounded-lg border border-border bg-surface px-3 text-sm text-foreground",
      "placeholder:text-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
      "disabled:cursor-not-allowed disabled:opacity-60",
      invalid && "border-danger focus-visible:ring-danger",
      className,
    )}
    {...props}
  />
));
Input.displayName = "Input";
