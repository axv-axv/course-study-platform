package com.courseplatform.backend.auth;

import com.courseplatform.backend.user.UserRole;
import com.courseplatform.backend.user.UserRepository;
import com.courseplatform.backend.user.UserStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : previewToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(token);
                long userId = Long.parseLong(claims.getSubject());
                var currentUser = users.findById(userId)
                        .filter(user -> user.status() == UserStatus.ACTIVE)
                        .orElseThrow(() -> new IllegalArgumentException("用户不可用"));
                UserRole role = currentUser.role();
                AuthenticatedUser principal = new AuthenticatedUser(userId, currentUser.username(), role);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String previewToken(HttpServletRequest request) {
        return request.getMethod().equals("GET")
                && request.getRequestURI().matches("/api/v1/files/\\d+/preview")
                ? request.getParameter("token") : null;
    }
}
