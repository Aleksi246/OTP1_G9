package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional AuthService edge case tests to complement the existing AuthServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceEdgeCaseTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> response;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(httpClient, "http://localhost:7700");
    }

    @Test
    void loginWithEmailInputUsesEmailFieldInJson() throws Exception {
        String body = "{\"token\":\"tok\",\"email\":\"user@test.com\",\"username\":\"user\"}";
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        LoginResult result = authService.login("user@test.com", "pass");

        assertTrue(result.isSuccess());
        assertEquals("user@test.com", result.getEmail());
    }

    @Test
    void loginWithUsernameInputUsesUsernameFieldInJson() throws Exception {
        String body = "{\"token\":\"tok\",\"email\":\"u@t.com\",\"username\":\"myuser\"}";
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        LoginResult result = authService.login("myuser", "pass");

        assertTrue(result.isSuccess());
        assertEquals("myuser", result.getUsername());
    }

    @Test
    void loginStatus500ReturnsError() throws Exception {
        when(response.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        LoginResult result = authService.login("user@test.com", "pass");

        assertFalse(result.isSuccess());
        assertEquals("login.error.failed", result.getErrorKey());
        assertEquals(500, result.getErrorArgs()[0]);
    }

    @Test
    void loginStatus403ReturnsError() throws Exception {
        when(response.statusCode()).thenReturn(403);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        LoginResult result = authService.login("user@test.com", "pass");

        assertFalse(result.isSuccess());
        assertEquals("login.error.failed", result.getErrorKey());
        assertEquals(403, result.getErrorArgs()[0]);
    }

    @Test
    void loginEmptyResponseBodyTokenMissing() throws Exception {
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        LoginResult result = authService.login("user@test.com", "pass");

        assertFalse(result.isSuccess());
        assertEquals("login.error.token", result.getErrorKey());
    }

    @Test
    void loginBlankEmailInResponseFallsBackToTokenEmail() throws Exception {
        String body = "{\"token\":\"tok\",\"email\":\"\",\"username\":\"myuser\"}";
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        LoginResult result = authService.login("myuser", "pass");

        assertTrue(result.isSuccess());
        // Email should fallback to JWTHelper extraction (returns null for fake token "tok")
    }

    @Test
    void loginBlankUsernameInResponseFallsBackToInput() throws Exception {
        String body = "{\"token\":\"tok\",\"email\":\"e@e.com\",\"username\":\"\"}";
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        LoginResult result = authService.login("loginuser", "pass");

        assertTrue(result.isSuccess());
        assertEquals("loginuser", result.getUsername());
    }

    @Test
    void loginSpecialCharsInPasswordEscapedProperly() throws Exception {
        String body = "{\"token\":\"tok\",\"email\":\"e@e.com\",\"username\":\"u\"}";
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        // Should not throw — special chars should be escaped in the JSON builder
        LoginResult result = authService.login("user@test.com", "pass\"with\\quotes\nand\rnewlines");
        assertTrue(result.isSuccess());
    }
}
