package com.aji.accounting.user;

import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.user.UserDtos.RoleResponse;
import com.aji.accounting.user.UserDtos.UpdateRolesRequest;
import com.aji.accounting.user.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Pengguna")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER:VIEW')")
    public PageResponse<UserResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) UserStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.search(q, status, page, size);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER:VIEW')")
    public UserResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/users/{id}/approve")
    @PreAuthorize("hasAuthority('USER:APPROVE')")
    public UserResponse approve(@PathVariable Long id, @Valid @RequestBody UpdateRolesRequest req) {
        return service.approve(id, req.roleCodes());
    }

    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('USER:EDIT')")
    public UserResponse updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateRolesRequest req) {
        return service.updateRoles(id, req.roleCodes());
    }

    @PostMapping("/users/{id}/activate")
    @PreAuthorize("hasAuthority('USER:EDIT')")
    public UserResponse activate(@PathVariable Long id) {
        return service.setActive(id, true);
    }

    @PostMapping("/users/{id}/deactivate")
    @PreAuthorize("hasAuthority('USER:EDIT')")
    public UserResponse deactivate(@PathVariable Long id) {
        return service.setActive(id, false);
    }

    @PostMapping("/users/{id}/unlock")
    @PreAuthorize("hasAuthority('USER:EDIT')")
    public UserResponse unlock(@PathVariable Long id) {
        return service.unlock(id);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('USER:VIEW')")
    public List<RoleResponse> roles() {
        return service.roles();
    }
}
