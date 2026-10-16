import { SuspensePage } from "@/components/shared/suspense-page";
import { PaymentDetail } from "./detail";

export default function Page() {
  return (
    <SuspensePage>
      <PaymentDetail />
    </SuspensePage>
  );
}
