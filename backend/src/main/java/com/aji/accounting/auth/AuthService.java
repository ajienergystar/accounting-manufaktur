package com.aji.accounting.auth;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.auth.AuthDtos.ChangePasswordRequest;
import com.aji.accounting.auth.AuthDtos.CompanyInfo;
import com.aji.accounting.auth.AuthDtos.LoginRequest;
import com.aji.accounting.auth.AuthDtos.MeResponse;
import com.aji.accounting.auth.AuthDtos.RegisterRequest;
import com.aji.accounting.auth.AuthDtos.RegisterResponse;
import com.aji.accounting.auth.AuthDtos.ResetPasswordRequest;
import com.aji.accounting.common.exception.AuthFailedException;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.FieldValidationException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.company.Company;
import com.aji.accounting.company.CompanyRepository;
import com.aji.accounting.config.AppProperties;
import com.aji.accounting.config.security.AppUserPrincipal;
import com.aji.accounting.config.security.AuthCookies;
import com.aji.accounting.config.security.JwtService;
import com.aji.accounting.user.Role;
import com.aji.accounting.user.RoleRepository;
import com.aji.accounting.user.User;
import com.aji.accounting.user.UserRepository;
import com.aji.accounting.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private static final String GENERIC_LOGIN_ERROR = "Email/username atau password salah.";
    /** Toleransi refresh bersamaan dari beberapa tab browser. */
    private static final Duration REFRESH_REUSE_GRACE = Duration.ofSeconds(30);
    private static final Duration SESSION_REFRESH_TTL = Duration.ofDays(1);

    private final UserRepository users;
    private final RoleRepository roles;
    private final CompanyRepository companies;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthCookies cookies;
    private final AuditService audit;
    private final AppProperties props;
    private final String dummyHash;

    public AuthService(UserRepository users, RoleRepository roles, CompanyRepository companies,
                       RefreshTokenRepository refreshTokens, PasswordResetTokenRepository resetTokens,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AuthCookies cookies,
                       AuditService audit, AppProperties props) {
        this.users = users;
        this.roles = roles;
        this.companies = companies;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookies = cookies;
        this.audit = audit;
        this.props = props;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-" + TokenGenerator.randomToken());
    }

    // ------------------------------------------------------------------ registrasi

    @Transactional(readOnly = true)
    public boolean hasUsers() {
        return users.count() > 0;
    }

    /**
     * User pertama otomatis menjadi ADMIN dan membuat data perusahaan.
     * Registrasi berikutnya berstatus PENDING sampai di-approve Admin.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new FieldValidationException("confirmPassword", "Konfirmasi password tidak sama");
        }
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new FieldValidationException("username", "Username sudah digunakan");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new FieldValidationException("email", "Email sudah terdaftar");
        }

        boolean firstUser = users.count() == 0;
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(req.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setPasswordChangedAt(LocalDateTime.now());

        if (firstUser) {
            if (req.companyName() == null || req.companyName().isBlank()) {
                throw new FieldValidationException("companyName", "Nama perusahaan wajib diisi untuk pendaftar pertama");
            }
            Company company = companies.findFirstByOrderByIdAsc().orElseGet(() -> {
                Company c = new Company();
                c.setCode("MAIN");
                c.setName(req.companyName().trim());
                return companies.save(c);
            });
            user.setCompany(company);
            user.setStatus(UserStatus.ACTIVE);
            user.getRoles().add(role(Role.ADMIN));
        } else {
            Company company = companies.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new IllegalStateException("Data perusahaan belum ada"));
            user.setCompany(company);
            user.setStatus(UserStatus.PENDING);
            user.getRoles().add(role(Role.VIEWER));
        }
        users.save(user);
        audit.logAs(username, AuditAction.CREATE, "User", user.getId(),
                firstUser ? "Registrasi administrator pertama" : "Registrasi user baru (menunggu persetujuan)");

        return firstUser
                ? new RegisterResponse("Registrasi berhasil. Anda terdaftar sebagai Administrator, silakan login.", "ACTIVE")
                : new RegisterResponse("Registrasi berhasil. Akun Anda menunggu persetujuan administrator.", "PENDING");
    }

    private Role role(String code) {
        return roles.findByCode(code).orElseThrow(() -> new IllegalStateException("Role " + code + " belum di-seed"));
    }

    // ------------------------------------------------------------------ login / refresh / logout

    @Transactional(noRollbackFor = AuthFailedException.class)
    public MeResponse login(LoginRequest req, HttpServletRequest httpReq, HttpServletResponse httpRes) {
        LocalDateTime now = LocalDateTime.now();
        Optional<User> found = users.findByIdentifierWithAuthorities(req.identifier().trim());
        if (found.isEmpty()) {
            passwordEncoder.matches(req.password(), dummyHash); // samakan waktu respons
            throw new AuthFailedException(GENERIC_LOGIN_ERROR);
        }
        User user = found.get();

        if (user.isLocked(now)) {
            long minutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
            throw new AuthFailedException("Akun terkunci sementara karena terlalu banyak percobaan gagal. Coba lagi dalam "
                    + minutes + " menit.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            audit.logAs(user.getUsername(), AuditAction.LOGIN_FAILED, "User", user.getId(),
                    "Login gagal ke-" + attempts);
            if (attempts >= props.security().maxFailedAttempts()) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(now.plusMinutes(props.security().lockMinutes()));
                throw new AuthFailedException("Akun terkunci " + props.security().lockMinutes()
                        + " menit karena " + attempts + " kali gagal login.");
            }
            user.setFailedLoginAttempts(attempts);
            throw new AuthFailedException(GENERIC_LOGIN_ERROR);
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new AuthFailedException("Akun Anda masih menunggu persetujuan administrator.");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AuthFailedException("Akun Anda dinonaktifkan. Hubungi administrator.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        issueTokens(user, req.rememberMe(), httpReq, httpRes);
        audit.logAs(user.getUsername(), AuditAction.LOGIN, "User", user.getId(), "Login berhasil");
        return toMe(user);
    }

    @Transactional(noRollbackFor = AuthFailedException.class)
    public MeResponse refresh(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        LocalDateTime now = LocalDateTime.now();
        String raw = cookies.read(httpReq, AuthCookies.REFRESH)
                .orElseThrow(() -> new AuthFailedException("Sesi tidak ditemukan. Silakan login kembali."));
        RefreshToken token = refreshTokens.findByTokenHash(TokenGenerator.sha256(raw)).orElse(null);
        if (token == null || token.getExpiresAt().isBefore(now)) {
            cookies.clear(httpRes);
            throw new AuthFailedException("Sesi sudah berakhir. Silakan login kembali.");
        }

        if (token.getRevokedAt() != null) {
            boolean benignRace = token.getReplacedByHash() != null
                    && token.getRevokedAt().isAfter(now.minus(REFRESH_REUSE_GRACE));
            if (!benignRace) {
                // Token lama dipakai ulang: indikasi pencurian token → cabut semua sesi user.
                refreshTokens.revokeAllForUser(token.getUser().getId(), now);
                cookies.clear(httpRes);
                log.warn("Refresh token reuse terdeteksi untuk user id {}", token.getUser().getId());
                throw new AuthFailedException("Sesi tidak valid. Silakan login kembali.");
            }
        }

        User user = users.findByIdWithAuthorities(token.getUser().getId())
                .orElseThrow(() -> new AuthFailedException("Sesi tidak valid."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokens.revokeAllForUser(user.getId(), now);
            cookies.clear(httpRes);
            throw new AuthFailedException("Akun tidak aktif.");
        }

        String newRaw = issueTokens(user, token.isRememberMe(), httpReq, httpRes);
        if (token.getRevokedAt() == null) {
            token.setRevokedAt(now);
            token.setReplacedByHash(TokenGenerator.sha256(newRaw));
        }
        return toMe(user);
    }

    @Transactional
    public void logout(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        cookies.read(httpReq, AuthCookies.REFRESH)
                .flatMap(raw -> refreshTokens.findByTokenHash(TokenGenerator.sha256(raw)))
                .ifPresent(t -> {
                    if (t.getRevokedAt() == null) {
                        t.setRevokedAt(LocalDateTime.now());
                    }
                    audit.logAs(t.getUser().getUsername(), AuditAction.LOGOUT, "User", t.getUser().getId(), "Logout");
                });
        cookies.clear(httpRes);
    }

    /** Membuat access token + refresh token baru dan menulisnya ke cookie. @return refresh token mentah. */
    private String issueTokens(User user, boolean rememberMe, HttpServletRequest httpReq, HttpServletResponse httpRes) {
        LocalDateTime now = LocalDateTime.now();
        Duration refreshTtl = rememberMe ? Duration.ofDays(props.jwt().refreshTokenDays()) : SESSION_REFRESH_TTL;

        String access = jwtService.generateAccessToken(AppUserPrincipal.from(user));
        String raw = TokenGenerator.randomToken();

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(TokenGenerator.sha256(raw));
        rt.setExpiresAt(now.plus(refreshTtl));
        rt.setRememberMe(rememberMe);
        rt.setCreatedAt(now);
        rt.setIpAddress(AuditServiceIp.of(httpReq));
        String ua = httpReq.getHeader(HttpHeaders.USER_AGENT);
        rt.setUserAgent(ua == null ? null : ua.substring(0, Math.min(ua.length(), 255)));
        refreshTokens.save(rt);

        cookies.write(httpRes, access, jwtService.accessTtl(), raw, refreshTtl, rememberMe);
        return raw;
    }

    // ------------------------------------------------------------------ profil & password

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        return users.findByIdWithAuthorities(userId).map(this::toMe)
                .orElseThrow(() -> NotFoundException.of("User", userId));
    }

    /** Selalu mengembalikan pesan yang sama agar tidak membocorkan email terdaftar atau tidak. */
    @Transactional
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCase(email.trim()).filter(u -> u.getStatus() == UserStatus.ACTIVE).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now();
            resetTokens.invalidateAllForUser(user.getId(), now);
            String raw = TokenGenerator.randomToken();
            PasswordResetToken t = new PasswordResetToken();
            t.setUser(user);
            t.setTokenHash(TokenGenerator.sha256(raw));
            t.setCreatedAt(now);
            t.setExpiresAt(now.plusMinutes(props.security().resetTokenMinutes()));
            resetTokens.save(t);
            // Catatan: integrasi email (JavaMailSender) dapat ditambahkan di sini; saat ini link dicetak ke log.
            log.info("[RESET PASSWORD] Link untuk {}: {}/reset-password?token={} (berlaku {} menit)",
                    user.getEmail(), props.frontendUrl(), raw, props.security().resetTokenMinutes());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new FieldValidationException("confirmPassword", "Konfirmasi password tidak sama");
        }
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken t = resetTokens.findByTokenHash(TokenGenerator.sha256(req.token()))
                .filter(x -> x.getUsedAt() == null && x.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN",
                        "Link reset password tidak valid atau sudah kedaluwarsa. Silakan minta link baru."));
        User user = t.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setPasswordChangedAt(now);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        t.setUsedAt(now);
        refreshTokens.revokeAllForUser(user.getId(), now);
        audit.logAs(user.getUsername(), AuditAction.PASSWORD_RESET, "User", user.getId(), "Reset password via email");
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req, HttpServletRequest httpReq,
                               HttpServletResponse httpRes) {
        User user = users.findByIdWithAuthorities(userId).orElseThrow(() -> NotFoundException.of("User", userId));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new FieldValidationException("currentPassword", "Password lama salah");
        }
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new FieldValidationException("confirmPassword", "Konfirmasi password tidak sama");
        }
        if (req.newPassword().equals(req.currentPassword())) {
            throw new FieldValidationException("newPassword", "Password baru harus berbeda dari password lama");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setPasswordChangedAt(now);
        // Cabut semua sesi lain, lalu terbitkan sesi baru untuk perangkat ini.
        refreshTokens.revokeAllForUser(user.getId(), now);
        issueTokens(user, false, httpReq, httpRes);
        audit.log(AuditAction.PASSWORD_CHANGE, "User", user.getId(), "Ganti password");
    }

    MeResponse toMe(User u) {
        Company c = u.getCompany();
        return new MeResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                new CompanyInfo(c.getId(), c.getCode(), c.getName()), u.roleCodes(), u.permissionCodes());
    }

    /** Helper kecil agar AuthService tidak bergantung pada RequestContextHolder. */
    static final class AuditServiceIp {
        private AuditServiceIp() {
        }

        static String of(HttpServletRequest req) {
            String fwd = req.getHeader("X-Forwarded-For");
            return fwd != null && !fwd.isBlank() ? fwd.split(",")[0].trim() : req.getRemoteAddr();
        }
    }
}
