"use client";

import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, XCircle } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { api, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";

interface Health {
  status: string;
  time: string;
  database: string;
}

export default function DashboardPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["health"],
    queryFn: () => api.get<Health>("/health"),
  });

  return (
    <>
      <PageHeader title="Dashboard" description="Ringkasan kondisi keuangan dan produksi perusahaan." />
      <Card className="max-w-lg">
        <CardHeader>
          <CardTitle>Status Sistem</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-12 w-full" />
          ) : error ? (
            <p className="flex items-center gap-2 text-sm text-danger">
              <XCircle className="h-4 w-4" /> Backend tidak terhubung: {errorMessage(error)}
            </p>
          ) : (
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
              <dt className="text-muted">Status</dt>
              <dd className="flex items-center gap-1.5 font-medium text-success">
                <CheckCircle2 className="h-4 w-4" /> {data?.status}
              </dd>
              <dt className="text-muted">Database</dt>
              <dd>MySQL {data?.database}</dd>
              <dt className="text-muted">Waktu server</dt>
              <dd>{formatDateTime(data?.time)}</dd>
            </dl>
          )}
        </CardContent>
      </Card>
    </>
  );
}
