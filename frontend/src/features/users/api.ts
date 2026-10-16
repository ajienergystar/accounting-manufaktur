import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { PageResponse } from "@/types/common";

export type UserStatus = "PENDING" | "ACTIVE" | "INACTIVE";

export interface UserRow {
  id: number;
  username: string;
  email: string;
  fullName: string;
  status: UserStatus;
  roles: string[];
  locked: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface RoleRow {
  id: number;
  code: string;
  name: string;
  description: string;
  systemRole: boolean;
  permissions: string[];
}

export function useUsers(params: { q?: string; status?: UserStatus | ""; page: number }) {
  return useQuery({
    queryKey: ["users", params],
    queryFn: () => api.get<PageResponse<UserRow>>("/users", { ...params, size: 20 }),
    placeholderData: (prev) => prev,
  });
}

export function useRoles() {
  return useQuery({ queryKey: ["roles"], queryFn: () => api.get<RoleRow[]>("/roles"), staleTime: 10 * 60_000 });
}

export function useUserAction() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (a: { id: number; action: "approve" | "roles" | "activate" | "deactivate" | "unlock"; roleCodes?: string[] }) => {
      switch (a.action) {
        case "approve":
          return api.post<UserRow>(`/users/${a.id}/approve`, { roleCodes: a.roleCodes });
        case "roles":
          return api.put<UserRow>(`/users/${a.id}/roles`, { roleCodes: a.roleCodes });
        default:
          return api.post<UserRow>(`/users/${a.id}/${a.action}`);
      }
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
  });
}
