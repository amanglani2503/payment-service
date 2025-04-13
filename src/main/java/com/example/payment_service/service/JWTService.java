package com.example.payment_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JWTService {

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    // Extracts the role claim from the JWT
    public String extractRole(String token) {
        try {
            return extractAllClaims(token).get("role", String.class);
        } catch (Exception e) {
            logger.error("Failed to extract role from token", e);
            return null;
        }
    }

    // Extracts all claims from the JWT
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            logger.error("Failed to extract claims from token", e);
            throw e;
        }
    }

    // Generates the secret key used to verify the JWT signature
    private SecretKey getKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid secret key format", e);
            throw e;
        }
    }

    // Extracts the username (subject) from the JWT
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            logger.error("Failed to extract username from token", e);
            return null;
        }
    }

    // Generic method to extract a specific claim using a resolver function
    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    // Validates the token by checking username match and token expiration
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String userName = extractUsername(token);
            boolean isValid = userName != null && userName.equals(userDetails.getUsername()) && !isTokenExpired(token);
            if (!isValid) {
                logger.warn("Token validation failed for user: {}", userDetails.getUsername());
            }
            return isValid;
        } catch (ExpiredJwtException e) {
            logger.warn("Token expired for user: {}", userDetails.getUsername(), e);
            return false;
        } catch (JwtException e) {
            logger.error("Invalid JWT token", e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error during token validation", e);
            return false;
        }
    }

    // Checks whether the token is expired
    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            logger.warn("Could not determine token expiration", e);
            return true;
        }
    }

    // Extracts expiration time from the JWT
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
