package com.aji.accounting.attachment;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.config.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Penyimpanan lampiran bukti transaksi di filesystem lokal (app.storage.dir). */
@Service
public class AttachmentService {

    public record AttachmentResponse(Long id, String fileName, String contentType, long sizeBytes, String uploadedBy,
                                     LocalDateTime uploadedAt) {
        static AttachmentResponse from(Attachment a) {
            return new AttachmentResponse(a.getId(), a.getFileName(), a.getContentType(), a.getSizeBytes(),
                    a.getUploadedBy(), a.getUploadedAt());
        }
    }

    /** Tipe entitas yang boleh diberi lampiran → modul permission-nya. */
    private static final Map<String, String> ENTITY_MODULES = Map.ofEntries(
            Map.entry("JOURNAL", "LEDGER"),
            Map.entry("PURCHASE_ORDER", "PURCHASE"),
            Map.entry("GOODS_RECEIPT", "PURCHASE"),
            Map.entry("PURCHASE_INVOICE", "PURCHASE"),
            Map.entry("AP_PAYMENT", "PURCHASE"),
            Map.entry("SALES_ORDER", "SALES"),
            Map.entry("DELIVERY_ORDER", "SALES"),
            Map.entry("SALES_INVOICE", "SALES"),
            Map.entry("AR_RECEIPT", "SALES"),
            Map.entry("CASH_TRANSACTION", "CASHBANK"),
            Map.entry("FIXED_ASSET", "ASSET"),
            Map.entry("WORK_ORDER", "PRODUCTION"),
            Map.entry("STOCK_OPNAME", "INVENTORY"));

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/webp", "text/csv", "text/plain",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final long MAX_SIZE = 10L * 1024 * 1024;

    private final AttachmentRepository repo;
    private final AuditService audit;
    private final Path root;

    public AttachmentService(AttachmentRepository repo, AuditService audit,
                             @Value("${app.storage.dir:./data/attachments}") String dir) {
        this.repo = repo;
        this.audit = audit;
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(String entityType, Long entityId) {
        checkPermission(entityType, "VIEW");
        return repo.findByEntityTypeAndEntityIdOrderByUploadedAtDesc(entityType, entityId).stream()
                .map(AttachmentResponse::from).toList();
    }

    @Transactional
    public AttachmentResponse upload(String entityType, Long entityId, MultipartFile file) {
        checkPermission(entityType, "EDIT");
        if (file.isEmpty()) {
            throw new BusinessException("File kosong");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("Ukuran file maksimal 10 MB");
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("Tipe file tidak didukung. Gunakan PDF, gambar, Excel, Word, atau CSV.");
        }
        String original = sanitize(file.getOriginalFilename());
        LocalDate today = LocalDate.now();
        String key = "%d/%02d/%s-%s".formatted(today.getYear(), today.getMonthValue(), UUID.randomUUID(), original);
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("Nama file tidak valid");
        }
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal menyimpan file", e);
        }
        Attachment a = new Attachment();
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setFileName(original);
        a.setContentType(contentType);
        a.setSizeBytes(file.getSize());
        a.setStorageKey(key);
        a.setUploadedBy(CurrentUser.usernameOrSystem());
        a.setUploadedAt(LocalDateTime.now());
        repo.save(a);
        audit.log(AuditAction.CREATE, "Lampiran", a.getId(), "Upload " + original + " ke " + entityType + " #" + entityId);
        return AttachmentResponse.from(a);
    }

    @Transactional(readOnly = true)
    public Attachment get(Long id) {
        Attachment a = repo.findById(id).orElseThrow(() -> NotFoundException.of("Lampiran", id));
        checkPermission(a.getEntityType(), "VIEW");
        return a;
    }

    public Resource resource(Attachment a) {
        Path p = root.resolve(a.getStorageKey()).normalize();
        if (!p.startsWith(root) || !Files.exists(p)) {
            throw new NotFoundException("File lampiran tidak ditemukan di penyimpanan");
        }
        return new PathResource(p);
    }

    @Transactional
    public void delete(Long id) {
        Attachment a = repo.findById(id).orElseThrow(() -> NotFoundException.of("Lampiran", id));
        checkPermission(a.getEntityType(), "EDIT");
        repo.delete(a);
        try {
            Files.deleteIfExists(root.resolve(a.getStorageKey()).normalize());
        } catch (IOException ignored) {
            // file yatim tidak mengganggu konsistensi data
        }
        audit.log(AuditAction.DELETE, "Lampiran", id, "Hapus lampiran " + a.getFileName());
    }

    private void checkPermission(String entityType, String action) {
        String module = ENTITY_MODULES.get(entityType);
        if (module == null) {
            throw new BusinessException("Tipe entitas lampiran tidak dikenal: " + entityType);
        }
        String needed = module + ":" + action;
        boolean ok = CurrentUser.principal()
                .map(p -> p.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(needed::equals))
                .orElse(false);
        if (!ok) {
            throw new AccessDeniedException("Butuh permission " + needed);
        }
    }

    private static String sanitize(String name) {
        String n = name == null ? "file" : Path.of(name).getFileName().toString();
        n = n.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (n.isEmpty()) {
            n = "file";
        }
        return n.length() > 150 ? n.substring(n.length() - 150) : n;
    }
}
