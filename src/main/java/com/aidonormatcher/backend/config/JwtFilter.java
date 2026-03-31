package com.aidonormatcher.backend.config;

import com.aidonormatcher.backend.service.JwtService;
import com.aidonormatcher.backend.service.FirebaseAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.aidonormatcher.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final FirebaseAuthService firebaseAuthService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            if (!tryAuthenticateWithInternalJwt(token)) {
                tryAuthenticateWithFirebase(token);
            }
        }
        chain.doFilter(request, response);
    }

    private boolean tryAuthenticateWithInternalJwt(String token) {
        try {
            String email = jwtService.extractEmail(token);
            if (email == null) {
                return false;
            }
            UserDetails user = userDetailsService.loadUserByUsername(email);
            if (jwtService.isValid(token, user)) {
                setAuthentication(user);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private boolean tryAuthenticateWithFirebase(String token) {
        try {
            Optional<User> user = firebaseAuthService.resolveAuthenticatedUser(token);
            user.ifPresent(this::setAuthentication);
            return user.isPresent();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void setAuthentication(UserDetails user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
