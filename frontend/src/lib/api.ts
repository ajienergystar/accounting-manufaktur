/**
 * HTTP client ke backend Spring Boot (lewat rewrites /api → :8080).
 * Token disimpan di cookie httpOnly oleh backend, jadi client cukup kirim credentials.
 * Bila 401, coba refresh token sekali lalu ulangi request; jika gagal, redirect ke /login.
 */

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  code?: string;
  errors?: Record<string, string>;
}

export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail;

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail || problem.title || `Request gagal (${status})`);
    this.status = status;
    this.problem = problem;
  }
}

type QueryValue = string | number | boolean | null | undefined;

export interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  query?: Record<string, QueryValue>;
  /** Jangan coba refresh token saat 401 (dipakai oleh endpoint auth sendiri). */
  skipAuthRefresh?: boolean;
}

const BASE = "/api/v1";

let refreshPromise: Promise<boolean> | null = null;

async function refreshSession(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = fetch(`${BASE}/auth/refresh`, { method: "POST", credentials: "include" })
      .then((r) => r.ok)
      .catch(() => false)
      .finally(() => {
        setTimeout(() => (refreshPromise = null), 0);
      });
  }
  return refreshPromise;
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const url = path.startsWith("http") ? path : `${BASE}${path}`;
  if (!query) return url;
  const params = new URLSearchParams();
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== null && v !== "") params.append(k, String(v));
  }
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

async function parseError(res: Response): Promise<ApiError> {
  let problem: ProblemDetail = { status: res.status };
  try {
    problem = { ...problem, ...(await res.json()) };
  } catch {
    problem.detail = res.statusText;
  }
  return new ApiError(res.status, problem);
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, query, skipAuthRefresh, headers, ...init } = options;
  const isForm = typeof FormData !== "undefined" && body instanceof FormData;

  const doFetch = () =>
    fetch(buildUrl(path, query), {
      ...init,
      credentials: "include",
      headers: {
        Accept: "application/json",
        ...(body !== undefined && !isForm ? { "Content-Type": "application/json" } : {}),
        ...headers,
      },
      body: body === undefined ? undefined : isForm ? (body as FormData) : JSON.stringify(body),
    });

  let res = await doFetch();

  if (res.status === 401 && !skipAuthRefresh) {
    const refreshed = await refreshSession();
    if (refreshed) {
      res = await doFetch();
    } else if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
      const next = encodeURIComponent(window.location.pathname + window.location.search);
      // Sengaja hard-redirect: sesi habis, semua state client harus dibuang.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.location.href = `/login?next=${next}`;
    }
  }

  if (!res.ok) throw await parseError(res);
  if (res.status === 204) return undefined as T;

  const contentType = res.headers.get("content-type") ?? "";
  if (contentType.includes("application/json") || contentType.includes("problem+json")) {
    return (await res.json()) as T;
  }
  return (await res.blob()) as unknown as T;
}

export const api = {
  get: <T>(path: string, query?: Record<string, QueryValue>, opts?: RequestOptions) =>
    apiRequest<T>(path, { ...opts, method: "GET", query }),
  post: <T>(path: string, body?: unknown, opts?: RequestOptions) =>
    apiRequest<T>(path, { ...opts, method: "POST", body }),
  put: <T>(path: string, body?: unknown, opts?: RequestOptions) =>
    apiRequest<T>(path, { ...opts, method: "PUT", body }),
  patch: <T>(path: string, body?: unknown, opts?: RequestOptions) =>
    apiRequest<T>(path, { ...opts, method: "PATCH", body }),
  delete: <T>(path: string, opts?: RequestOptions) => apiRequest<T>(path, { ...opts, method: "DELETE" }),
};

/** Unduh file (PDF/Excel) dari endpoint export. */
export async function downloadFile(path: string, query: Record<string, QueryValue>, filename: string) {
  const blob = await apiRequest<Blob>(path, { method: "GET", query, headers: { Accept: "*/*" } });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return "Terjadi kesalahan";
}
