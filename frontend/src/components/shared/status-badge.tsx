import { Badge } from "@/components/ui/badge";
import { STATUS_LABEL } from "@/features/purchasing/types";

export function StatusBadge({ status }: { status: string }) {
  const s = STATUS_LABEL[status] ?? { label: status, tone: "neutral" as const };
  return <Badge tone={s.tone}>{s.label}</Badge>;
}
