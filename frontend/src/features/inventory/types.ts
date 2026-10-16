export interface AdjustmentLine {
  itemId: number;
  itemCode: string;
  itemName: string;
  uomCode: string;
  qtyChange: string;
  unitCost: string;
  totalValue: string;
  note: string | null;
}
export interface Adjustment {
  id: number;
  docNo: string;
  docDate: string;
  warehouseId: number;
  warehouseName: string;
  adjustmentType: "ADJUSTMENT" | "OPENING" | "OPNAME";
  reason: string;
  status: string;
  totalValue: string;
  journalId: number | null;
  lines: AdjustmentLine[];
}
export interface Transfer {
  id: number;
  docNo: string;
  docDate: string;
  fromWarehouseId: number;
  fromWarehouseName: string;
  toWarehouseId: number;
  toWarehouseName: string;
  notes: string | null;
  status: string;
  totalValue: string;
  lines: { itemId: number; itemCode: string; itemName: string; uomCode: string; qty: string; unitCost: string; totalValue: string }[];
}
export interface Opname {
  id: number;
  docNo: string;
  docDate: string;
  warehouseId: number;
  warehouseName: string;
  notes: string | null;
  status: "DRAFT" | "POSTED" | "VOID";
  adjustmentId: number | null;
  adjustmentNo: string | null;
  countedLines: number;
  totalLines: number;
  totalDiffValue: string;
  lines: {
    itemId: number;
    itemCode: string;
    itemName: string;
    uomCode: string;
    systemQty: string;
    countedQty: string | null;
    diffQty: string;
    unitCost: string;
    diffValue: string;
    note: string | null;
  }[];
}
export const ADJ_TYPE_LABEL = { ADJUSTMENT: "Penyesuaian", OPENING: "Saldo awal", OPNAME: "Hasil opname" } as const;
