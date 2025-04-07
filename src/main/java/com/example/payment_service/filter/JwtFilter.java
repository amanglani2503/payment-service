package com.example.payment_service.filter;

import com.example.payment_service.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JWTService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        String role = null;

        // Extract token from Authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
            username = jwtService.extractUsername(token);
            role = jwtService.extractRole(token);
        }

        // Debugging logs
        System.out.println("Auth Header: " + authHeader);
        System.out.println("Extracted Username: " + username);
        System.out.println("Extracted Role: " + role);

        // Validate token and authenticate user
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Ensure the role has the correct "ROLE_" prefix
            String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            UserDetails userDetails = new User(username, "", List.of(new SimpleGrantedAuthority(formattedRole)));

            System.out.println("Assigned Authorities: " + userDetails.getAuthorities());

            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
