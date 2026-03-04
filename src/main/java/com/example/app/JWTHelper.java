package com.example.app;

import java.util.Base64;

public class JWTHelper {

    public static String getEmailFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            String subKey = "\"sub\":";
            int start = payload.indexOf(subKey);
            if (start == -1) {
                return null;
            }
            start += subKey.length();

            StringBuilder email = new StringBuilder();
            boolean inQuotes = false;
            for (int i = start; i < payload.length(); i++) {
                char c = payload.charAt(i);
                if (c == '"') {
                    if (inQuotes) {
                        break;
                    }
                    inQuotes = true;
                } else if (inQuotes) {
                    email.append(c);
                }
            }

            return email.toString();
        } catch (Exception e) {
            System.err.println("Error extracting email from token: " + e.getMessage());
            return null;
        }
    }

    public static String getUserTypeFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            String userTypeKey = "\"userType\":";
            int start = payload.indexOf(userTypeKey);
            if (start == -1) {
                return null;
            }
            start += userTypeKey.length();

            StringBuilder userType = new StringBuilder();
            boolean inQuotes = false;
            for (int i = start; i < payload.length(); i++) {
                char c = payload.charAt(i);
                if (c == '"') {
                    if (inQuotes) {
                        break;
                    }
                    inQuotes = true;
                } else if (inQuotes) {
                    userType.append(c);
                }
            }

            return userType.toString().isEmpty() ? "student" : userType.toString();
        } catch (Exception e) {
            System.err.println("Error extracting userType from token: " + e.getMessage());
            return "student";
        }
    }
}
