"use client";

import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef,
  type SortingState,
} from "@tanstack/react-table";
import { ArrowDown, ArrowUp, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState, ErrorState } from "./states";
import { cn } from "@/lib/utils";

export interface DataTableProps<T> {
  columns: ColumnDef<T, unknown>[];
  data: T[] | undefined;
  loading?: boolean;
  error?: string | null;
  onRetry?: () => void;
  /** Pagination server-side (0-based). */
  page?: number;
  pageSize?: number;
  totalElements?: number;
  totalPages?: number;
  onPageChange?: (page: number) => void;
  sorting?: SortingState;
  onSortingChange?: (s: SortingState) => void;
  emptyTitle?: string;
  emptyDescription?: string;
  onRowClick?: (row: T) => void;
  footer?: React.ReactNode;
}

export function DataTable<T>({
  columns,
  data,
  loading,
  error,
  onRetry,
  page = 0,
  pageSize = 20,
  totalElements,
  totalPages,
  onPageChange,
  sorting,
  onSortingChange,
  emptyTitle = "Belum ada data",
  emptyDescription,
  onRowClick,
  footer,
}: DataTableProps<T>) {
  // TanStack Table mengembalikan fungsi yang tidak bisa di-memoize; aman diabaikan untuk React Compiler.
  // eslint-disable-next-line react-hooks/incompatible-library
  const table = useReactTable({
    data: data ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualSorting: true,
    manualPagination: true,
    state: { sorting: sorting ?? [] },
    onSortingChange: (updater) => {
      if (!onSortingChange) return;
      const next = typeof updater === "function" ? updater(sorting ?? []) : updater;
      onSortingChange(next);
    },
    enableSorting: Boolean(onSortingChange),
  });

  if (error) return <ErrorState message={error} onRetry={onRetry} />;

  const rows = table.getRowModel().rows;
  const from = totalElements ? page * pageSize + 1 : 0;
  const to = totalElements ? Math.min((page + 1) * pageSize, totalElements) : 0;

  return (
    <div>
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-sm">
          <thead>
            {table.getHeaderGroups().map((hg) => (
              <tr key={hg.id} className="border-b border-border bg-surface-muted/60">
                {hg.headers.map((h) => {
                  const sortable = h.column.getCanSort();
                  const dir = h.column.getIsSorted();
                  const meta = h.column.columnDef.meta as { align?: "right" | "center"; className?: string } | undefined;
                  return (
                    <th
                      key={h.id}
                      scope="col"
                      className={cn(
                        "whitespace-nowrap px-4 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-muted",
                        meta?.align === "right" && "text-right",
                        meta?.align === "center" && "text-center",
                        meta?.className,
                      )}
                      aria-sort={dir === "asc" ? "ascending" : dir === "desc" ? "descending" : undefined}
                    >
                      {h.isPlaceholder ? null : sortable ? (
                        <button
                          type="button"
                          className="inline-flex items-center gap-1 hover:text-foreground"
                          onClick={h.column.getToggleSortingHandler()}
                        >
                          {flexRender(h.column.columnDef.header, h.getContext())}
                          {dir === "asc" && <ArrowUp className="h-3 w-3" />}
                          {dir === "desc" && <ArrowDown className="h-3 w-3" />}
                        </button>
                      ) : (
                        flexRender(h.column.columnDef.header, h.getContext())
                      )}
                    </th>
                  );
                })}
              </tr>
            ))}
          </thead>
          <tbody>
            {loading
              ? Array.from({ length: 6 }).map((_, i) => (
                  <tr key={i} className="border-b border-border">
                    {columns.map((_c, j) => (
                      <td key={j} className="px-4 py-3">
                        <Skeleton className="h-4 w-full" />
                      </td>
                    ))}
                  </tr>
                ))
              : rows.map((row) => (
                  <tr
                    key={row.id}
                    onClick={onRowClick ? () => onRowClick(row.original) : undefined}
                    className={cn(
                      "border-b border-border last:border-0 hover:bg-surface-muted/50",
                      onRowClick && "cursor-pointer",
                    )}
                  >
                    {row.getVisibleCells().map((cell) => {
                      const meta = cell.column.columnDef.meta as
                        | { align?: "right" | "center"; className?: string }
                        | undefined;
                      return (
                        <td
                          key={cell.id}
                          className={cn(
                            "px-4 py-2.5 align-middle",
                            meta?.align === "right" && "num",
                            meta?.align === "center" && "text-center",
                            meta?.className,
                          )}
                        >
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      );
                    })}
                  </tr>
                ))}
          </tbody>
          {footer && <tfoot>{footer}</tfoot>}
        </table>
      </div>

      {!loading && rows.length === 0 && <EmptyState title={emptyTitle} description={emptyDescription} />}

      {onPageChange && totalPages !== undefined && totalPages > 0 && (
        <div className="no-print flex items-center justify-between border-t border-border px-4 py-3 text-sm text-muted">
          <span>
            {from}–{to} dari {totalElements}
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page <= 0}
              onClick={() => onPageChange(page - 1)}
              aria-label="Halaman sebelumnya"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span>
              Hal. {page + 1} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= totalPages}
              onClick={() => onPageChange(page + 1)}
              aria-label="Halaman berikutnya"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
