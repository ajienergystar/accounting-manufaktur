import { Check, X } from "lucide-react";
import { PASSWORD_RULES } from "./schemas";
import { cn } from "@/lib/utils";

const LEVELS = [
  { label: "Sangat lemah", color: "bg-danger" },
  { label: "Lemah", color: "bg-danger" },
  { label: "Cukup", color: "bg-warning" },
  { label: "Baik", color: "bg-warning" },
  { label: "Kuat", color: "bg-success" },
  { label: "Sangat kuat", color: "bg-success" },
];

export function PasswordStrength({ value }: { value: string }) {
  const passed = PASSWORD_RULES.filter((r) => r.test(value)).length;
  const bonus = value.length >= 12 && passed === PASSWORD_RULES.length ? 1 : 0;
  const score = Math.min(passed + bonus, 5);
  const level = LEVELS[score];

  return (
    <div className="mt-2" aria-live="polite">
      <div className="flex gap-1" aria-hidden>
        {Array.from({ length: 5 }).map((_, i) => (
          <span key={i} className={cn("h-1.5 flex-1 rounded-full bg-surface-muted", i < score && level.color)} />
        ))}
      </div>
      <p className="mt-1 text-xs text-muted">
        Kekuatan: <span className="font-medium text-foreground">{value ? level.label : "-"}</span>
      </p>
      <ul className="mt-1.5 grid grid-cols-2 gap-x-3 gap-y-0.5">
        {PASSWORD_RULES.map((r) => {
          const ok = r.test(value);
          return (
            <li key={r.label} className={cn("flex items-center gap-1 text-xs", ok ? "text-success" : "text-muted")}>
              {ok ? <Check className="h-3 w-3" /> : <X className="h-3 w-3" />}
              {r.label}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
