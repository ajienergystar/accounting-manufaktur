export interface Ref {
  id: number;
  code: string;
  name: string;
}

export const STATUS_LABEL: Record<string, { label: string; tone: "neutral" | "success" | "warning" | "danger" | "info" | "primary" }> = {
  DRAFT: { label: "Draft", tone: "warning" },
  SUBMITTED: { label: "Diajukan", tone: "info" },
  APPROVED: { label: "Disetujui", tone: "primary" },
  REJECTED: { label: "Ditolak", tone: "danger" },
  ORDERED: { label: "Sudah PO", tone: "success" },
  PARTIAL: { label: "Sebagian", tone: "info" },
  RECEIVED: { label: "Diterima", tone: "success" },
  CLOSED: { label: "Ditutup", tone: "neutral" },
  CANCELLED: { label: "Dibatalkan", tone: "neutral" },
  POSTED: { label: "Posted", tone: "success" },
  VOID: { label: "Batal", tone: "neutral" },
  UNPAID: { label: "Belum dibayar", tone: "warning" },
  PAID: { label: "Lunas", tone: "success" },
};

export interface PoLine {
  id: number;
  lineNo: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  description: string | null;
  qty: string;
  unitPrice: string;
  discountPct: string;
  lineTotal: string;
  qtyReceived: string;
  remainingQty: string;
  netUnitPrice: string;
}

export interface PurchaseOrder {
  id: number;
  docNo: string;
  docDate: string;
  supplier: Ref;
  warehouse: Ref;
  requestId: number | null;
  requestNo: string | null;
  expectedDate: string | null;
  taxRateId: number | null;
  taxRate: string;
  subtotal: string;
  discountAmount: string;
  taxAmount: string;
  total: string;
  notes: string | null;
  status: string;
  approvedBy: string | null;
  approvedAt: string | null;
  lines: PoLine[];
}

export interface GrnLine {
  id: number;
  lineNo: number;
  orderLineId: number | null;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  qty: string;
  unitPrice: string;
  freightAlloc: string;
  unitCost: string;
  totalCost: string;
  qtyInvoiced: string;
  qtyReturned: string;
}

export interface GoodsReceipt {
  id: number;
  docNo: string;
  docDate: string;
  supplier: Ref;
  orderId: number | null;
  orderNo: string | null;
  warehouse: Ref;
  deliveryNoteNo: string | null;
  freightCost: string;
  totalValue: string;
  notes: string | null;
  status: string;
  journalId: number | null;
  lines: GrnLine[];
}

export interface UninvoicedLine {
  receiptLineId: number;
  receiptId: number;
  receiptNo: string;
  receiptDate: string;
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  qtyOpen: string;
  unitPrice: string;
  unitCost: string;
}

export interface PiLine {
  id: number;
  lineNo: number;
  receiptLineId: number | null;
  receiptNo: string | null;
  itemId: number | null;
  itemCode: string | null;
  accountId: number | null;
  accountCode: string | null;
  accountName: string | null;
  departmentId: number | null;
  departmentName: string | null;
  description: string;
  qty: string;
  unitPrice: string;
  lineTotal: string;
  receiptCost: string;
}

export interface PurchaseInvoice {
  id: number;
  docNo: string;
  docDate: string;
  dueDate: string;
  supplier: Ref;
  supplierInvoiceNo: string;
  taxInvoiceNo: string | null;
  taxRateId: number | null;
  taxRate: string;
  pphRateId: number | null;
  pphRate: string;
  subtotal: string;
  discountAmount: string;
  freightAmount: string;
  dpp: string;
  taxAmount: string;
  pphAmount: string;
  total: string;
  payableAmount: string;
  paidAmount: string;
  returnedAmount: string;
  outstanding: string;
  notes: string | null;
  status: string;
  journalId: number | null;
  daysOverdue: number;
  lines: PiLine[];
  payments: { paymentId: number; paymentNo: string; date: string; type: string; amount: string }[];
}

export interface PurchaseReturn {
  id: number;
  docNo: string;
  docDate: string;
  supplier: Ref;
  receiptId: number;
  receiptNo: string;
  invoiceId: number | null;
  invoiceNo: string | null;
  warehouse: Ref;
  reason: string;
  subtotal: string;
  taxAmount: string;
  total: string;
  status: string;
  journalId: number | null;
  lines: { id: number; receiptLineId: number; itemId: number; itemCode: string; itemName: string; uomCode: string; qty: string; unitCost: string; totalCost: string }[];
}

export interface ApPayment {
  id: number;
  docNo: string;
  docDate: string;
  supplier: Ref;
  paymentType: "INVOICE" | "ADVANCE" | "ADVANCE_OFFSET";
  cashAccount: Ref | null;
  amount: string;
  bankCharges: string;
  referenceNo: string | null;
  notes: string | null;
  status: string;
  journalId: number | null;
  allocations: { invoiceId: number; invoiceNo: string; supplierInvoiceNo: string; amount: string }[];
}

export interface PurchaseRequest {
  id: number;
  docNo: string;
  docDate: string;
  department: Ref | null;
  requestedBy: string;
  neededDate: string | null;
  notes: string | null;
  status: string;
  approvedBy: string | null;
  approvedAt: string | null;
  lines: { id: number; itemId: number; itemCode: string; itemName: string; uomCode: string; qty: string; note: string | null }[];
}

export const PAYMENT_TYPE_LABEL = {
  INVOICE: "Pelunasan faktur",
  ADVANCE: "Uang muka",
  ADVANCE_OFFSET: "Kompensasi uang muka",
} as const;
