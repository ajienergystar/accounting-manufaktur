import {
  BarChart3,
  BookOpen,
  Boxes,
  Building2,
  Calculator,
  Factory,
  Landmark,
  LayoutDashboard,
  Package,
  PiggyBank,
  Receipt,
  Settings,
  ShoppingCart,
  Truck,
  Users,
  Wallet,
  type LucideIcon,
} from "lucide-react";

export interface NavItem {
  label: string;
  href: string;
  /** Permission minimal untuk melihat menu, contoh "LEDGER:VIEW". Kosong = semua user login. */
  permission?: string;
}

export interface NavGroup {
  label: string;
  icon: LucideIcon;
  items: NavItem[];
}

export const NAV: NavGroup[] = [
  {
    label: "Ringkasan",
    icon: LayoutDashboard,
    items: [{ label: "Dashboard", href: "/dashboard" }],
  },
  {
    label: "Master Data",
    icon: Building2,
    items: [
      { label: "Item / Barang", href: "/master/item", permission: "MASTER:VIEW" },
      { label: "Pelanggan", href: "/master/pelanggan", permission: "MASTER:VIEW" },
      { label: "Pemasok", href: "/master/pemasok", permission: "MASTER:VIEW" },
      { label: "Gudang", href: "/master/gudang", permission: "MASTER:VIEW" },
      { label: "Departemen", href: "/master/departemen", permission: "MASTER:VIEW" },
      { label: "Satuan", href: "/master/satuan", permission: "MASTER:VIEW" },
      { label: "Pajak", href: "/master/pajak", permission: "MASTER:VIEW" },
    ],
  },
  {
    label: "Buku Besar",
    icon: BookOpen,
    items: [
      { label: "Bagan Akun", href: "/coa", permission: "COA:VIEW" },
      { label: "Jurnal Umum", href: "/jurnal", permission: "LEDGER:VIEW" },
      { label: "Jurnal Berulang", href: "/jurnal/berulang", permission: "LEDGER:VIEW" },
      { label: "Saldo Awal", href: "/jurnal/saldo-awal", permission: "LEDGER:POST" },
      { label: "Buku Besar", href: "/jurnal/buku-besar", permission: "LEDGER:VIEW" },
      { label: "Neraca Saldo", href: "/jurnal/neraca-saldo", permission: "LEDGER:VIEW" },
      { label: "Periode Akuntansi", href: "/jurnal/periode", permission: "PERIOD:VIEW" },
    ],
  },
  {
    label: "Pembelian",
    icon: ShoppingCart,
    items: [
      { label: "Permintaan Pembelian", href: "/pembelian/pr", permission: "PURCHASE:VIEW" },
      { label: "Purchase Order", href: "/pembelian/po", permission: "PURCHASE:VIEW" },
      { label: "Penerimaan Barang", href: "/pembelian/penerimaan", permission: "PURCHASE:VIEW" },
      { label: "Faktur Pembelian", href: "/pembelian/faktur", permission: "PURCHASE:VIEW" },
      { label: "Retur Pembelian", href: "/pembelian/retur", permission: "PURCHASE:VIEW" },
      { label: "Pembayaran Utang", href: "/pembelian/pembayaran", permission: "PURCHASE:VIEW" },
      { label: "Umur Utang (Aging)", href: "/pembelian/aging", permission: "PURCHASE:VIEW" },
    ],
  },
  {
    label: "Persediaan",
    icon: Boxes,
    items: [
      { label: "Stok", href: "/persediaan", permission: "INVENTORY:VIEW" },
      { label: "Kartu Stok", href: "/persediaan/kartu-stok", permission: "INVENTORY:VIEW" },
      { label: "Penyesuaian & Transfer", href: "/persediaan/mutasi", permission: "INVENTORY:VIEW" },
      { label: "Stock Opname", href: "/persediaan/opname", permission: "INVENTORY:VIEW" },
    ],
  },
  {
    label: "Produksi & Biaya",
    icon: Factory,
    items: [
      { label: "Bill of Materials", href: "/produksi/bom", permission: "PRODUCTION:VIEW" },
      { label: "Work Center & Tarif BOP", href: "/produksi/work-center", permission: "PRODUCTION:VIEW" },
      { label: "Work Order", href: "/produksi/work-order", permission: "PRODUCTION:VIEW" },
      { label: "Process Costing", href: "/produksi/process-costing", permission: "PRODUCTION:VIEW" },
      { label: "Analisis Selisih", href: "/produksi/selisih", permission: "PRODUCTION:VIEW" },
    ],
  },
  {
    label: "Penjualan",
    icon: Truck,
    items: [
      { label: "Sales Order", href: "/penjualan/so", permission: "SALES:VIEW" },
      { label: "Pengiriman", href: "/penjualan/pengiriman", permission: "SALES:VIEW" },
      { label: "Faktur Penjualan", href: "/penjualan/faktur", permission: "SALES:VIEW" },
      { label: "Penerimaan Piutang", href: "/penjualan/penerimaan", permission: "SALES:VIEW" },
    ],
  },
  {
    label: "Kas & Bank",
    icon: Wallet,
    items: [
      { label: "Rekening Kas/Bank", href: "/kas-bank", permission: "CASHBANK:VIEW" },
      { label: "Transaksi Kas", href: "/kas-bank/transaksi", permission: "CASHBANK:VIEW" },
      { label: "Rekonsiliasi Bank", href: "/kas-bank/rekonsiliasi", permission: "CASHBANK:VIEW" },
    ],
  },
  {
    label: "Aset Tetap",
    icon: Landmark,
    items: [{ label: "Daftar Aset", href: "/aset-tetap", permission: "ASSET:VIEW" }],
  },
  {
    label: "Pajak & Anggaran",
    icon: PiggyBank,
    items: [
      { label: "Rekap PPN", href: "/pajak/ppn", permission: "TAX:VIEW" },
      { label: "Anggaran", href: "/anggaran", permission: "BUDGET:VIEW" },
    ],
  },
  {
    label: "Laporan",
    icon: BarChart3,
    items: [
      { label: "Laporan Keuangan", href: "/laporan", permission: "REPORT:VIEW" },
      { label: "Laporan Biaya Produksi", href: "/laporan/biaya", permission: "REPORT:VIEW" },
    ],
  },
  {
    label: "Pengaturan",
    icon: Settings,
    items: [
      { label: "Perusahaan", href: "/pengaturan/perusahaan", permission: "SETTINGS:VIEW" },
      { label: "Account Mapping", href: "/pengaturan/account-mapping", permission: "SETTINGS:VIEW" },
      { label: "Pengguna", href: "/pengaturan/pengguna", permission: "USER:VIEW" },
      { label: "Audit Trail", href: "/pengaturan/audit", permission: "AUDIT:VIEW" },
    ],
  },
];

export const QUICK_ICONS = { Users, Package, Receipt, Calculator };
