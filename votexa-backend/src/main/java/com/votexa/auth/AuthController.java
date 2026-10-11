package com.votexa.auth;

import com.votexa.auth.AuthDtos.LoginRequest;
import com.votexa.auth.AuthDtos.LoginResponse;
import com.votexa.common.ApiException;
import com.votexa.voter.VoterRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final VoterRepository voters;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, VoterRepository voters, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.voters = voters; this.encoder = encoder; this.jwt = jwt;
    }

    @PostMapping("/login")
    @Transactional
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        UserAccount u = users.findByUsername(req.username().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!u.isEnabled()) throw new ApiException(HttpStatus.UNAUTHORIZED, "Account disabled");
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            u.setFailedLoginAttempts(u.getFailedLoginAttempts() + 1);
            users.save(u);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        u.setFailedLoginAttempts(0);
        u.setLastLoginAt(Instant.now());
        users.save(u);
        String voterCode = voters.findByCitizenUserId(u.getId()).map(v -> v.getVoterCode()).orElse(null);
        return new LoginResponse(jwt.generate(u), u.getUsername(), u.getFullName(), u.getRole().getName(), voterCode);
    }

    @GetMapping("/me")
    public AuthUser me(@AuthenticationPrincipal AuthUser me) { return me; }
}
