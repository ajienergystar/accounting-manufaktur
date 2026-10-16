package com.aji.accounting.user;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.auth.RefreshTokenRepository;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.config.CurrentUser;
import com.aji.accounting.user.UserDtos.RoleResponse;
import com.aji.accounting.user.UserDtos.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, UserStatus status, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return PageResponse.of(users.search(q == null || q.isBlank() ? null : q.trim(), status, pageable),
                UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(load(id));
    }

    @Transactional
    public UserResponse approve(Long id, Set<String> roleCodes) {
        User user = load(id);
        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException("Hanya user berstatus PENDING yang dapat disetujui");
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(resolveRoles(roleCodes));
        audit.log(AuditAction.APPROVE, "User", id, "Menyetujui user " + user.getUsername() + " dengan role " + roleCodes);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateRoles(Long id, Set<String> roleCodes) {
        User user = load(id);
        Set<String> before = user.roleCodes();
        boolean removingAdmin = user.hasRole(Role.ADMIN) && !roleCodes.contains(Role.ADMIN);
        if (removingAdmin) {
            ensureNotLastActiveAdmin(user);
        }
        user.setRoles(resolveRoles(roleCodes));
        audit.log(AuditAction.UPDATE, "User", id, "Ubah role user " + user.getUsername(), before, roleCodes);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User user = load(id);
        if (!active) {
            if (user.getId().equals(CurrentUser.id())) {
                throw new BusinessException("Anda tidak dapat menonaktifkan akun sendiri");
            }
            if (user.hasRole(Role.ADMIN)) {
                ensureNotLastActiveAdmin(user);
            }
            user.setStatus(UserStatus.INACTIVE);
            refreshTokens.revokeAllForUser(user.getId(), LocalDateTime.now());
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }
        audit.log(AuditAction.UPDATE, "User", id, (active ? "Aktifkan" : "Nonaktifkan") + " user " + user.getUsername());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse unlock(Long id) {
        User user = load(id);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        audit.log(AuditAction.UPDATE, "User", id, "Buka kunci user " + user.getUsername());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roles.findAllByOrderByIdAsc().stream().map(RoleResponse::from).toList();
    }

    private void ensureNotLastActiveAdmin(User user) {
        if (user.getStatus() == UserStatus.ACTIVE && users.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new BusinessException("Harus ada minimal satu Administrator aktif");
        }
    }

    private Set<Role> resolveRoles(Set<String> codes) {
        List<Role> found = roles.findByCodeIn(codes);
        if (found.size() != codes.size()) {
            throw new BusinessException("Terdapat role yang tidak dikenal");
        }
        return new HashSet<>(found);
    }

    private User load(Long id) {
        return users.findByIdWithAuthorities(id).orElseThrow(() -> NotFoundException.of("User", id));
    }
}
