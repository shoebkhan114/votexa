package com.votexa.admin;

import com.votexa.audit.AuditService;
import com.votexa.auth.*;
import com.votexa.common.ApiException;
import com.votexa.common.Roles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize(Roles.ADMIN)
public class AdminController {
    public record UserDto(Long id, String username, String fullName, String email, String role, boolean enabled) {}
    public record CreateUser(@NotBlank @Size(min = 3, max = 50) String username,
                             @NotBlank @Size(min = 8, max = 100) String password,
                             @NotBlank String fullName, String email,
                             @NotBlank @Pattern(regexp = "CITIZEN|BLO|ERO|POLLING_OFFICER|AUDITOR|SUPER_ADMIN") String role) {}

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AdminController(UserRepository users, RoleRepository roles, PasswordEncoder encoder, AuditService audit) {
        this.users = users; this.roles = roles; this.encoder = encoder; this.audit = audit;
    }

    private UserDto dto(UserAccount u) {
        return new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole().getName(), u.isEnabled());
    }

    @GetMapping("/users")
    public List<UserDto> list() { return users.findAll().stream().map(this::dto).toList(); }

    @PostMapping("/users")
    @Transactional
    public UserDto create(@Valid @RequestBody CreateUser r, @AuthenticationPrincipal AuthUser me) {
        if (users.existsByUsername(r.username())) throw ApiException.conflict("Username already exists");
        UserAccount u = new UserAccount();
        u.setUsername(r.username().trim());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setEmail(r.email() == null || r.email().isBlank() ? null : r.email().trim());
        u.setRole(roles.findByName(r.role()).orElseThrow(() -> ApiException.badRequest("Unknown role")));
        users.save(u);
        audit.record("AUDIT_EVENT", "USER", u.getUsername(), me, "USER_CREATED", null, r.role(), null, "SUCCESS");
        return dto(u);
    }

    @PostMapping("/users/{id}/toggle")
    @Transactional
    public UserDto toggle(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        UserAccount u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (u.getId().equals(me.id())) throw ApiException.badRequest("You cannot disable your own account");
        u.setEnabled(!u.isEnabled());
        u.setFailedLoginAttempts(0);
        users.save(u);
        audit.record("AUDIT_EVENT", "USER", u.getUsername(), me, u.isEnabled() ? "USER_ENABLED" : "USER_DISABLED", null, null, null, "SUCCESS");
        return dto(u);
    }
}
