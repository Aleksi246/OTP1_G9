package com.example.service;

public final class LoginResult {

    private final boolean success;
    private final String token;
    private final String username;
    private final String email;
    private final String userType;
    private final String errorKey;
    private final Object[] errorArgs;

    private LoginResult(boolean success, String token, String username, String email, String userType, String errorKey, Object[] errorArgs) {
        this.success = success;
        this.token = token;
        this.username = username;
        this.email = email;
        this.userType = userType;
        this.errorKey = errorKey;
        this.errorArgs = errorArgs;
    }

    public static LoginResult success(String token, String username, String email, String userType) {
        return new LoginResult(true, token, username, email, userType, null, null);
    }

    public static LoginResult error(String errorKey, Object... args) {
        return new LoginResult(false, null, null, null, null, errorKey, args);
    }

    public boolean isSuccess() { return success; }
    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getUserType() { return userType; }
    public String getErrorKey() { return errorKey; }
    public Object[] getErrorArgs() { return errorArgs; }
}
