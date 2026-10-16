export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MessageResponse {
  message: string;
}

/** Nilai uang dikirim/diterima sebagai string desimal agar tidak kehilangan presisi. */
export type Decimal = string;
