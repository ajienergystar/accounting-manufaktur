export const ITEM_TYPES = {
  RAW_MATERIAL: "Bahan Baku",
  SUPPORTING_MATERIAL: "Bahan Penolong",
  WORK_IN_PROCESS: "Barang Dalam Proses",
  FINISHED_GOOD: "Barang Jadi",
  SUPPLIES: "Supplies",
  SERVICE: "Jasa",
} as const;
export type ItemType = keyof typeof ITEM_TYPES;

export const ITEM_TYPE_TONE: Record<ItemType, "primary" | "info" | "warning" | "success" | "neutral"> = {
  RAW_MATERIAL: "primary",
  SUPPORTING_MATERIAL: "info",
  WORK_IN_PROCESS: "warning",
  FINISHED_GOOD: "success",
  SUPPLIES: "neutral",
  SERVICE: "neutral",
};
