package com.autograding.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() throws Exception {
        tokenProvider = new JwtTokenProvider();
        String key = Base64.getEncoder().encodeToString(
                "test-secret-key-at-least-32-bytes-long-.12345".getBytes());

        setField(tokenProvider, "secret", key);
        setField(tokenProvider, "expiration", 3600000L);
        tokenProvider.init();

        userDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    @Test
    void generateToken_shouldCreateValidJwt() {
        String token = tokenProvider.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void getUsernameFromToken_shouldExtractCorrectly() {
        String token = tokenProvider.generateToken(userDetails);

        String username = tokenProvider.getUsernameFromToken(token);

        assertEquals("testuser", username);
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = tokenProvider.generateToken(userDetails);

        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.string"));
    }

    @Test
    void validateToken_shouldReturnFalseForEmptyToken() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void validateToken_shouldReturnFalseForNullToken() {
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void validateToken_shouldReturnFalseForTamperedToken() {
        String token = tokenProvider.generateToken(userDetails);
        String tamperedToken = token.substring(0, token.length() - 4) + "xxxx";

        assertFalse(tokenProvider.validateToken(tamperedToken));
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() throws Exception {
        setField(tokenProvider, "expiration", 1L);
        tokenProvider.init();
        String token = tokenProvider.generateToken(userDetails);
        Thread.sleep(10);

        assertFalse(tokenProvider.validateToken(token));
    }

    @Test
    void generateToken_differentUsersProduceDifferentTokens() {
        UserDetails otherUser = new User("other", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER")));

        String token1 = tokenProvider.generateToken(userDetails);
        String token2 = tokenProvider.generateToken(otherUser);

        assertNotEquals(token1, token2);
        assertEquals("testuser", tokenProvider.getUsernameFromToken(token1));
        assertEquals("other", tokenProvider.getUsernameFromToken(token2));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
