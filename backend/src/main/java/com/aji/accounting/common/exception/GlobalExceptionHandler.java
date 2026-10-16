package com.aji.accounting.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Semua error API dikembalikan dalam format RFC 7807 (application/problem+json).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", OffsetDateTime.now());
        return pd;
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, "Data tidak ditemukan", ex.getMessage(), req);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest req) {
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Aturan bisnis dilanggar", ex.getMessage(), req);
        pd.setProperty("code", ex.getCode());
        return pd;
    }

    @ExceptionHandler(FieldValidationException.class)
    public ProblemDetail handleFieldValidation(FieldValidationException ex, HttpServletRequest req) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validasi gagal", ex.getMessage(), req);
        pd.setProperty("errors", ex.getErrors());
        return pd;
    }

    @ExceptionHandler(AuthFailedException.class)
    public ProblemDetail handleAuthFailed(AuthFailedException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNAUTHORIZED, "Autentikasi gagal", ex.getMessage(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNAUTHORIZED, "Belum login", "Sesi tidak valid atau sudah berakhir.", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return problem(HttpStatus.FORBIDDEN, "Akses ditolak", "Anda tidak memiliki hak akses untuk aksi ini.", req);
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, "Konflik data", ex.getMessage(), req);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, "Data sudah diubah",
                "Data telah diubah oleh pengguna lain. Muat ulang halaman lalu coba lagi.", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        String msg = ex.getMostSpecificCause().getMessage();
        String detail = msg != null && msg.contains("Duplicate entry")
                ? "Data duplikat: kode/nomor sudah digunakan."
                : "Data tidak dapat disimpan karena melanggar relasi atau keunikan data.";
        return problem(HttpStatus.CONFLICT, "Pelanggaran integritas data", detail, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> errors.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validasi gagal", "Periksa kembali isian form.", req);
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validasi gagal", "Parameter tidak valid.", req);
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ProblemDetail handleBadRequest(Exception ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "Permintaan tidak valid",
                ex instanceof IllegalArgumentException ? ex.getMessage() : "Format data permintaan tidak valid.", req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error at {}", req.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan sistem",
                "Terjadi kesalahan tak terduga. Silakan hubungi administrator.", req);
    }
}
