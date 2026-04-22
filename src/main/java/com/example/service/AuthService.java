package com.example.service;

import com.example.app.JWTHelper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuthService {

  private final HttpClient httpClient;
  private final String apiUrl;

  public AuthService(HttpClient httpClient, String apiUrl) {
    this.httpClient = httpClient;
    this.apiUrl = apiUrl;
  }

  public LoginResult login(String loginInput, String password)
      throws IOException, InterruptedException {
    String json = buildLoginJson(loginInput, password);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(apiUrl + "/api/auth/login"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    HttpResponse<String> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return handleResponse(response, loginInput);
  }

  // 🔹 Handle response logic (moved from controller)
  private LoginResult handleResponse(HttpResponse<String> response, String loginInput) {
    if (response.statusCode() != 200) {
      return LoginResult.error("login.error.failed", response.statusCode());
    }
    String body = response.body();
    String token = extractJsonField(body, "token");

    if (token == null) {
      return LoginResult.error("login.error.token");
    }

    String userType = JWTHelper.getUserTypeFromToken(token);
    String email = resolveEmail(body, token);
    String username = resolveUsername(body, loginInput);

    return LoginResult.success(token, username, email, userType);
  }

  // 🔹 JSON building
  private String buildLoginJson(String loginInput, String password) {
    String key = loginInput.contains("@") ? "email" : "username";
    return String.format(
        "{\"%s\":\"%s\",\"password\":\"%s\"}",
        key,
        escapeJson(loginInput),
        escapeJson(password)
    );
  }

  // 🔹 Fallback logic
  private String resolveEmail(String body, String token) {
    String email = extractJsonField(body, "email");
    return (email == null || email.isBlank())
        ? JWTHelper.getEmailFromToken(token)
        : email;
  }

  private String resolveUsername(String body, String loginInput) {
    String username = extractJsonField(body, "username");
    return (username == null || username.isBlank())
        ? loginInput
        : username;
  }

  // 🔹 Simple JSON parsing (you may replace later with Gson/Jackson)
  private String extractJsonField(String response, String fieldName) {
    String fieldPattern = "\"" + fieldName + "\":\"";
    int start = response.indexOf(fieldPattern);
    if (start == -1) return null;
    start += fieldPattern.length();
    int end = response.indexOf("\"", start);
    return end > start ? response.substring(start, end) : null;
  }

  private String escapeJson(String str) {
    return str
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
  }
}
