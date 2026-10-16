package com.aji.accounting.purchasing;

import com.aji.accounting.masterdata.supplier.Supplier;
import com.aji.accounting.purchasing.PurchasingDtos.AgingReport;
import com.aji.accounting.purchasing.PurchasingDtos.AgingRow;
import com.aji.accounting.purchasing.PurchasingDtos.SupplierSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Laporan umur utang (AP aging) berdasarkan hari lewat jatuh tempo. */
@Service
@RequiredArgsConstructor
public class ApReportService {

    private final PurchaseInvoiceRepository invoices;
    private final ApPaymentRepository payments;

    @Transactional(readOnly = true)
    public AgingReport aging(LocalDate asOf) {
        Map<Long, BigDecimal[]> buckets = new LinkedHashMap<>();
        Map<Long, Supplier> supplierById = new LinkedHashMap<>();
        for (PurchaseInvoice inv : invoices.findOutstanding(null)) {
            if (inv.getDocDate().isAfter(asOf)) {
                continue;
            }
            BigDecimal out = inv.outstanding();
            if (out.signum() <= 0) {
                continue;
            }
            Supplier s = inv.getSupplier();
            supplierById.putIfAbsent(s.getId(), s);
            BigDecimal[] b = buckets.computeIfAbsent(s.getId(), k -> zeros());
            long overdue = ChronoUnit.DAYS.between(inv.getDueDate(), asOf);
            int idx = overdue <= 0 ? 0 : overdue <= 30 ? 1 : overdue <= 60 ? 2 : overdue <= 90 ? 3 : 4;
            b[idx] = b[idx].add(out);
            b[5] = b[5].add(out);
        }
        List<AgingRow> rows = new ArrayList<>();
        BigDecimal[] t = zeros();
        buckets.forEach((id, b) -> {
            Supplier s = supplierById.get(id);
            rows.add(new AgingRow(id, s.getCode(), s.getName(), b[0], b[1], b[2], b[3], b[4], b[5]));
            for (int i = 0; i < 6; i++) {
                t[i] = t[i].add(b[i]);
            }
        });
        rows.sort(Comparator.comparing(AgingRow::total).reversed());
        return new AgingReport(asOf, rows, new AgingRow(null, null, "TOTAL", t[0], t[1], t[2], t[3], t[4], t[5]));
    }

    @Transactional(readOnly = true)
    public SupplierSummary supplierSummary(Long supplierId) {
        List<PurchaseInvoice> open = invoices.findOutstanding(supplierId);
        BigDecimal outstanding = open.stream().map(PurchaseInvoice::outstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SupplierSummary(supplierId, outstanding, payments.advanceBalance(supplierId), open.size());
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] a = new BigDecimal[6];
        java.util.Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }
}
