import { format, endOfMonth, startOfMonth } from "date-fns";

export function monthRange(d = new Date()) {
  return { from: format(startOfMonth(d), "yyyy-MM-dd"), to: format(endOfMonth(d), "yyyy-MM-dd") };
}

export function yearStart(d = new Date()) {
  return `${d.getFullYear()}-01-01`;
}
