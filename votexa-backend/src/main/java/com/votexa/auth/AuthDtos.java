package com.votexa.auth;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, String username, String fullName, String role, String voterCode) {}
}
