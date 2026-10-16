import { AuthenticatedShell } from "@/components/layout/authenticated-shell";

export default function DashboardLayout({ children }: LayoutProps<"/">) {
  return <AuthenticatedShell>{children}</AuthenticatedShell>;
}
