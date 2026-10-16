import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import { ApiError } from "./api";

/**
 * Pasang error validasi dari backend (ProblemDetail.errors) ke field form.
 * @returns true bila ada error field yang dipasang.
 */
export function applyServerErrors<T extends FieldValues>(err: unknown, setError: UseFormSetError<T>): boolean {
  if (!(err instanceof ApiError) || !err.problem.errors) return false;
  let applied = false;
  for (const [field, message] of Object.entries(err.problem.errors)) {
    setError(field as Path<T>, { type: "server", message });
    applied = true;
  }
  return applied;
}
