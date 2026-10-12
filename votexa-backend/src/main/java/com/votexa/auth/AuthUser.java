package com.votexa.auth;

/** Authenticated principal placed in the SecurityContext from the JWT. */
public record AuthUser(Long id, String username, String role, String fullName) {}
