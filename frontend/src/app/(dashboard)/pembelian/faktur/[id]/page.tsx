import { SuspensePage } from "@/components/shared/suspense-page";
import { PiDetail } from "./detail";

export default function Page() {
  return (
    <SuspensePage>
      <PiDetail />
    </SuspensePage>
  );
}
