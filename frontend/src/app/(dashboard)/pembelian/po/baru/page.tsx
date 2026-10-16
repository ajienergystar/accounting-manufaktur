import { SuspensePage } from "@/components/shared/suspense-page";
import { NewPo } from "./new-po";

export default function Page() {
  return (
    <SuspensePage>
      <NewPo />
    </SuspensePage>
  );
}
