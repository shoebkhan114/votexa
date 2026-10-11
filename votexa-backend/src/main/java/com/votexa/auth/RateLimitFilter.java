package com.votexa.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory limiter for the login endpoint: 10 attempts / minute / IP. */
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT = 10;
    private static final long WINDOW_MS = 60_000;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest r) {
        return !("POST".equals(r.getMethod()) && "/api/auth/login".equals(r.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Deque<Long> q = hits.computeIfAbsent(req.getRemoteAddr(), k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        boolean blocked;
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) q.pollFirst();
            blocked = q.size() >= LIMIT;
            if (!blocked) q.addLast(now);
        }
        if (blocked) { res.sendError(429, "Too many login attempts. Try again in a minute."); return; }
        chain.doFilter(req, res);
    }
}
