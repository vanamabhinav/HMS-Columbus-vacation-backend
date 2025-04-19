package com.Colombus.HotelManagement.Security;

import com.Colombus.HotelManagement.Models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
//import java.util.Base64;
import java.util.Date;
//import java.util.Optional;
import java.util.function.Function;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String SECRET_KEY_STRING = "your_very_long_secure_secret_key_that_is_32_bytes_or_more";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 hours

    //  Generate Token (Fixed)
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        logger.info("Generating token for user: {} with role: {}", user.getUserName(), user.getRole());
        
        // Ensure role is properly formatted with ROLE_ prefix
        String formattedRole = user.getRole();
        if (formattedRole != null && !formattedRole.startsWith("ROLE_")) {
            formattedRole = "ROLE_" + formattedRole;
            logger.info("Formatted role to: {}", formattedRole);
        }
        
        claims.put("role", formattedRole);
        return createToken(claims, user.getUserName());
    }

    // Create Token
    private String createToken(Map<String, Object> claims, String subject) {
        logger.debug("Creating token for subject: {}", subject);
        try {
            Date issuedAt = new Date(System.currentTimeMillis());
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION_TIME);
            
            logger.info("Token details - Subject: {}, Claims: {}, IssuedAt: {}, Expiration: {}", 
                subject, claims, issuedAt, expiration);
            
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .setIssuedAt(issuedAt)
                    .setExpiration(expiration)
                    .signWith(SECRET_KEY)
                    .compact();
            
            logger.info("Generated token (first 20 chars): {}", token.substring(0, Math.min(token.length(), 20)) + "...");
            return token;
        } catch (Exception e) {
            logger.error("Error creating token: {}", e.getMessage());
            throw e;
        }
    }

    // Extract Role from Token
    public String extractRole(String token) {
        try {
            String role = extractClaim(token, claims -> claims.get("role", String.class));
            logger.info("Extracted role from token: {}", role);
            return role;
        } catch (Exception e) {
            logger.error("Error extracting role from token: {}", e.getMessage());
            return null;
        }
    }

    //  Extract Username
    public String extractUsername(String token) {
        try {
            String username = extractClaim(token, Claims::getSubject);
            logger.debug("Extracted username from token: {}", username);
            return username;
        } catch (Exception e) {
            logger.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    //  Extract Expiration Date
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    //  Extract Any Claim (Fixed)
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            logger.error("Error extracting claim from token: {}", e.getMessage());
            throw e;
        }
    }

    // Validate Token
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            logger.info("Validating token for user: {}", userDetails.getUsername());
            
            // Extract the username from the token
            final String usernameFromToken = extractUsername(token);
            logger.info("Username from token: {}", usernameFromToken);
            
            // Extract expiration from token
            final Date expirationDate = extractExpiration(token);
            final boolean isExpired = expirationDate.before(new Date());
            logger.info("Token expiration date: {}, Is expired: {}", expirationDate, isExpired);
            
            // Extract role from token
            final String roleFromToken = extractRole(token);
            logger.info("Role from token: {}", roleFromToken);
            
            // Log user details information
            logger.info("User details - Username: {}, Authorities: {}", 
                    userDetails.getUsername(), userDetails.getAuthorities());
            
            // Check all validation criteria
            boolean usernameMatches = usernameFromToken != null && 
                                   usernameFromToken.equals(userDetails.getUsername());
            logger.info("Username matches: {}", usernameMatches);
            
            boolean isValid = usernameMatches && !isExpired;
            
            logger.info("Token validation result: {}", isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage(), e);
            return false;
        }
    }

    //  Check Expiration
    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            boolean isExpired = expiration.before(new Date());
            logger.info("Token expiration check - Expiration: {}, Current time: {}, Is expired: {}", 
                     expiration, new Date(), isExpired);
            return isExpired;
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage(), e);
            return true; // Treat as expired if there's an error
        }
    }
}
