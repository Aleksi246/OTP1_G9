package com.example.app;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Base64;

public class JWTHelper {

    private JWTHelper() {
          throw new UnsupportedOperationException("Utility class");
      }

    private static JsonObject decodePayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String json = new String(Base64.getUrlDecoder().decode(parts[1]));
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getEmailFromToken(String token) {
        JsonObject payload = decodePayload(token);
        if (payload == null || !payload.has("sub")) return null;
        return payload.get("sub").getAsString();
    }

    public static String getUserTypeFromToken(String token) {
        JsonObject payload = decodePayload(token);
        if (payload == null || !payload.has("userType")) return "student";
        String type = payload.get("userType").getAsString();
        return type.isEmpty() ? "student" : type;
    }
}
