package com.aji.accounting.user;

import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class UserDtos {
    private UserDtos() {
    }

    public record UserResponse(
            Long id,
            String username,
            String email,
            String fullName,
            UserStatus status,
            Set<String> roles,
            boolean locked,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt
    ) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getStatus(),
                    u.roleCodes(), u.isLocked(LocalDateTime.now()), u.getLastLoginAt(), u.getCreatedAt());
        }
    }

    public record UpdateRolesRequest(@NotEmpty(message = "Pilih minimal satu role") Set<String> roleCodes) {
    }

    public record PermissionResponse(String code, String module, String action, String description) {
        static PermissionResponse from(Permission p) {
            return new PermissionResponse(p.getCode(), p.getModule(), p.getAction(), p.getDescription());
        }
    }

    public record RoleResponse(Long id, String code, String name, String description, boolean systemRole,
                               List<String> permissions) {
        public static RoleResponse from(Role r) {
            return new RoleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(), r.isSystemRole(),
                    r.getPermissions().stream().map(Permission::getCode).sorted().toList());
        }
    }
}
