package com.example.app;

public class SessionManager {
  private static String currentUsername;
  private static String currentEmail;
  private static String currentToken;
  private static String currentUserType;
  private static Integer currentUserId;

  public static void setSession(String username, String email, String token, String userType) {
    currentUsername = username;
    currentEmail = email;
    currentToken = token;
    currentUserType = userType;
  }

  public static void setUserId(Integer userId) {
    currentUserId = userId;
  }

  public static void clearSession() {
    currentUsername = null;
    currentEmail = null;
    currentToken = null;
    currentUserType = null;
  }

  public static String getUsername() {
    return currentUsername;
  }

  public static String getEmail() {
    return currentEmail;
  }

  public static String getToken() {
    return currentToken;
  }

  public static String getUserType() {
    return currentUserType;
  }

  public static Integer getUserId() {
    return currentUserId;
  }

  public static boolean isLoggedIn() {
    return currentToken != null && !currentToken.isEmpty();
  }
}
