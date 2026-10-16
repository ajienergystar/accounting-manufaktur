import { SuspensePage } from "@/components/shared/suspense-page";
import { PaymentForm } from "./form";

export default function Page() {
  return (
    <SuspensePage>
      <PaymentForm />
    </SuspensePage>
  );
}
