package com.Colombus.HotelManagement.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

// Removed @Component annotation to prevent auto-detection
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            String method = request.getMethod();
            String authHeader = request.getHeader("Authorization");
            
            // Fully log request details
            logger.info("================= REQUEST DETAILS =================");
            logger.info("Processing request: {} {}", method, path);
            logger.info("Authorization header: {}", authHeader);
            logger.info("Query string: {}", request.getQueryString());
            
            // Skip token validation for OPTIONS requests and public endpoints
            if (method.equals("OPTIONS") || path.startsWith("/auth/login") || 
                path.startsWith("/auth/register") || path.startsWith("/auth/check-user") ||
                path.startsWith("/auth/validate") || path.equals("/error")) {
                logger.info("Skipping authentication for path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                logger.info("Token found - Username: {}, Role: {}", username, role);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    try {
                        logger.info("Loading user details for: {}", username);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        logger.info("User details loaded successfully from service: {}", username);
                        logger.info("User authorities: {}", userDetails.getAuthorities());
                        
                        if (jwtUtil.validateToken(token, userDetails)) {
                            logger.info("Token validated successfully for user: {}", username);
                            
                            // Create authentication with the exact authorities from userDetails
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, 
                                    null, 
                                    userDetails.getAuthorities()
                            );
                            
                            logger.info("Setting authentication with authorities: {}", userDetails.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            logger.info("Authentication set successfully for user: {}", username);
                            
                            // Add diagnostic information for pending-approvals endpoint
                            if (path.contains("/pending-approvals")) {
                                logger.info("ADMIN ENDPOINT ACCESS - /pending-approvals");
                                logger.info("User trying to access admin endpoint: {}", username);
                                logger.info("User authorities for admin endpoint: {}", userDetails.getAuthorities());
                                
                                boolean hasAdminRole = userDetails.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                                logger.info("Has ROLE_ADMIN authority: {}", hasAdminRole);
                            }
                        } else {
                            logger.warn("⚠️ Token validation FAILED for user: {}", username);
                        }
                    } catch (UsernameNotFoundException e) {
                        logger.error("❌ User not found in database: {}", username);
                    } catch (Exception e) {
                        logger.error("❌ Error loading user details: {}", e.getMessage(), e);
                    }
                } else {
                    if (username == null) {
                        logger.warn("⚠️ Username could not be extracted from token");
                    }
                    if (SecurityContextHolder.getContext().getAuthentication() != null) {
                        logger.info("Authentication already exists in SecurityContext");
                    }
                }
            } else {
                logger.info("No JWT token found in request or token format is invalid for path: {}", path);
            }
            
            // Log current authentication status before proceeding
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                logger.info("Current authentication: {}", SecurityContextHolder.getContext().getAuthentication());
                logger.info("Authenticated: {}", SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
                logger.info("Authorities: {}", SecurityContextHolder.getContext().getAuthentication().getAuthorities());
            } else {
                logger.info("No authentication in SecurityContext for path: {}", path);
            }
            
            logger.info("================= END REQUEST DETAILS =================");
        } catch (Exception e) {
            logger.error("❌ Error processing JWT token: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
