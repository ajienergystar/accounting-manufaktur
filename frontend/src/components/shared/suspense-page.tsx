import { Suspense, type ReactNode } from "react";
import { Skeleton } from "@/components/ui/skeleton";

/** Bungkus halaman client yang memakai useParams/useSearchParams (wajib Suspense di Cache Components). */
export function SuspensePage({ children }: { children: ReactNode }) {
  return <Suspense fallback={<Skeleton className="h-96 w-full" />}>{children}</Suspense>;
}
