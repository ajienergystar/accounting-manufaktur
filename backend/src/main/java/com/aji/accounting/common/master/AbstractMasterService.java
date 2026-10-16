package com.aji.accounting.common.master;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.entity.MasterEntity;
import com.aji.accounting.common.exception.ConflictException;
import com.aji.accounting.common.exception.FieldValidationException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.web.PageResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operasi CRUD standar master data: pencarian, validasi kode unik, audit, dan hapus aman
 * (bila sudah dipakai transaksi → tolak dan sarankan nonaktifkan).
 */
public abstract class AbstractMasterService<E extends MasterEntity, Q extends MasterRequest, R> {

    protected final MasterRepository<E> repository;
    protected final AuditService audit;

    @PersistenceContext
    protected EntityManager em;

    protected AbstractMasterService(MasterRepository<E> repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    protected abstract String entityName();

    protected abstract E newEntity();

    /** Salin field request ke entity (selain code/name/active yang ditangani di sini). */
    protected abstract void apply(Q req, E entity);

    protected abstract R toResponse(E entity);

    /** Hook validasi tambahan sebelum simpan. */
    protected void validate(Q req, E entity) {
    }

    @Transactional(readOnly = true)
    public PageResponse<R> search(String q, Boolean active, int page, int size, String sort) {
        Sort s = Sort.by(sort == null || sort.isBlank() ? "code" : sort);
        var pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200), s);
        return PageResponse.of(repository.search(q == null || q.isBlank() ? null : q.trim(), active, pageable),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<OptionResponse> options() {
        return repository.findByActiveTrueOrderByCodeAsc().stream().map(OptionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public R get(Long id) {
        return toResponse(load(id));
    }

    @Transactional
    public R create(Q req) {
        String code = normalizeCode(req.code());
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new FieldValidationException("code", "Kode " + code + " sudah digunakan");
        }
        E e = newEntity();
        e.setCode(code);
        e.setName(req.name().trim());
        e.setActive(req.active() == null || req.active());
        validate(req, e);
        apply(req, e);
        repository.save(e);
        audit.log(AuditAction.CREATE, entityName(), e.getId(), "Tambah " + entityName() + " " + code);
        return toResponse(e);
    }

    @Transactional
    public R update(Long id, Q req) {
        E e = load(id);
        String code = normalizeCode(req.code());
        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new FieldValidationException("code", "Kode " + code + " sudah digunakan");
        }
        R before = toResponse(e);
        e.setCode(code);
        e.setName(req.name().trim());
        if (req.active() != null) {
            e.setActive(req.active());
        }
        validate(req, e);
        apply(req, e);
        repository.flush();
        R after = toResponse(e);
        audit.log(AuditAction.UPDATE, entityName(), id, "Ubah " + entityName() + " " + code, before, after);
        return after;
    }

    @Transactional
    public void delete(Long id) {
        E e = load(id);
        try {
            repository.delete(e);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(entityName() + " " + e.getCode()
                    + " sudah dipakai di transaksi/data lain sehingga tidak bisa dihapus. Nonaktifkan saja.");
        }
        audit.log(AuditAction.DELETE, entityName(), id, "Hapus " + entityName() + " " + e.getCode());
    }

    protected E load(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of(entityName(), id));
    }

    protected static String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }
}
