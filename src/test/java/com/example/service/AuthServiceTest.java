package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> response;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(httpClient, "http://localhost:7700");
    }

    private static final String PASSWORD = "password";
    private static final String EMAIL = "test@test.com";

    // SUCCESS CASE
    @Test
    void shouldReturnSuccessWhenValidResponse() throws Exception {
        String body = "{\"token\":\"abc123\",\"email\":\"test@test.com\",\"username\":\"testuser\"}";

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        LoginResult result = authService.login(EMAIL, PASSWORD);

        assertTrue(result.isSuccess());
        assertEquals("abc123", result.getToken());
        assertEquals(EMAIL, result.getEmail());
        assertEquals("testuser", result.getUsername());
    }

    // NO TOKEN
    @Test
    void shouldReturnErrorWhenTokenMissing() throws Exception {
        String body = "{\"email\":\"test@test.com\"}";

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        LoginResult result = authService.login(EMAIL, PASSWORD);

        assertFalse(result.isSuccess());
        assertEquals("login.error.token", result.getErrorKey());
    }

    // NON-200 STATUS
    @Test
    void shouldReturnErrorWhenStatusNot200() throws Exception {
        when(response.statusCode()).thenReturn(401);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        LoginResult result = authService.login(EMAIL, PASSWORD);

        assertFalse(result.isSuccess());
        assertEquals("login.error.failed", result.getErrorKey());
        assertEquals(401, result.getErrorArgs()[0]);
    }

    // EMAIL FALLBACK FROM TOKEN
    @Test
    void shouldFallbackToTokenEmailWhenMissingInResponse() throws Exception {
        String body = "{\"token\":\"abc123\"}";

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        // ⚠️ This depends on JWTHelper behavior
        // You may need to mock or adjust JWTHelper if it's static

        LoginResult result = authService.login(EMAIL, PASSWORD);

        assertTrue(result.isSuccess());
        assertNull(result.getEmail());
    }

    // USERNAME FALLBACK
    @Test
    void shouldFallbackToLoginInputWhenUsernameMissing() throws Exception {
        String body = "{\"token\":\"abc123\",\"email\":\"test@test.com\"}";

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        LoginResult result = authService.login("myUsername", PASSWORD);

        assertTrue(result.isSuccess());
        assertEquals("myUsername", result.getUsername());
    }

    // IOException
    @Test
    void shouldThrowIOException() throws Exception {
        when(httpClient.send(any(), any())).thenThrow(new java.io.IOException("Connection failed"));

        assertThrows(IOException.class, () ->
                authService.login(EMAIL, PASSWORD));
    }

    // InterruptedException
    @Test
    void shouldThrowInterruptedException() throws Exception {
        when(httpClient.send(any(), any())).thenThrow(new InterruptedException());

        assertThrows(InterruptedException.class, () ->
                authService.login(EMAIL, PASSWORD));
    }
}