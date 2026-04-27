package com.example.app;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JWTHelperTest {

    /**
     * Builds a fake JWT token with the given payload JSON.
     * Real JWTs are header.payload.signature — we only need the payload for decoding.
     */
    private static String fakeToken(String payloadJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes());
        return header + "." + payload + ".fakesignature";
    }

    // =========================================================================
    // getEmailFromToken
    // =========================================================================

    @Test
    void getEmailFromTokenReturnsSubject() {
        String token = fakeToken("{\"sub\":\"alice@example.com\"}");
        assertEquals("alice@example.com", JWTHelper.getEmailFromToken(token));
    }

    @Test
    void getEmailFromTokenReturnsNullWhenNoSub() {
        String token = fakeToken("{\"name\":\"alice\"}");
        assertNull(JWTHelper.getEmailFromToken(token));
    }

    @Test
    void getEmailFromTokenReturnsNullForMalformedToken() {
        assertNull(JWTHelper.getEmailFromToken("not.a.valid.token"));
    }

    @Test
    void getEmailFromTokenReturnsNullForSinglePartToken() {
        assertNull(JWTHelper.getEmailFromToken("onlyonepart"));
    }

    @Test
    void getEmailFromTokenReturnsNullForEmptyString() {
        assertNull(JWTHelper.getEmailFromToken(""));
    }

    @Test
    void getEmailFromTokenReturnsNullForNull() {
        assertNull(JWTHelper.getEmailFromToken(null));
    }

    @Test
    void getEmailFromTokenReturnsNullForBadBase64() {
        assertNull(JWTHelper.getEmailFromToken("header.!!!invalid!!!.sig"));
    }

    // =========================================================================
    // getUserTypeFromToken
    // =========================================================================

    @Test
    void getUserTypeFromTokenReturnsUserType() {
        String token = fakeToken("{\"sub\":\"a@b.com\",\"userType\":\"teacher\"}");
        assertEquals("teacher", JWTHelper.getUserTypeFromToken(token));
    }

    @Test
    void getUserTypeFromTokenDefaultsToStudentWhenMissing() {
        String token = fakeToken("{\"sub\":\"a@b.com\"}");
        assertEquals("student", JWTHelper.getUserTypeFromToken(token));
    }

    @Test
    void getUserTypeFromTokenDefaultsToStudentWhenEmpty() {
        String token = fakeToken("{\"sub\":\"a@b.com\",\"userType\":\"\"}");
        assertEquals("student", JWTHelper.getUserTypeFromToken(token));
    }

    @Test
    void getUserTypeFromTokenDefaultsToStudentForBadToken() {
        assertEquals("student", JWTHelper.getUserTypeFromToken("garbage"));
    }

    @Test
    void getUserTypeFromTokenDefaultsToStudentForNull() {
        assertEquals("student", JWTHelper.getUserTypeFromToken(null));
    }

    // =========================================================================
    // Constructor is private utility class
    // =========================================================================

    @Test
    void constructorThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            var ctor = JWTHelper.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            try {
                ctor.newInstance();
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }
}
