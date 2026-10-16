import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api, errorMessage } from "@/lib/api";

/** Mutation standar aksi dokumen (approve, void, post, dll.) + invalidasi cache. */
export function useDocAction<T = unknown>(invalidate: string[], successMessage: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ path, body }: { path: string; body?: unknown }) => api.post<T>(path, body),
    onSuccess: () => {
      toast.success(successMessage);
      invalidate.forEach((k) => qc.invalidateQueries({ queryKey: [k] }));
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
}
