import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { JournalDetailPage } from "./journal-detail";

export default function Page() {
  return (
    <Suspense fallback={<Skeleton className="h-96 w-full" />}>
      <JournalDetailPage />
    </Suspense>
  );
}
