package com.example.app;

import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClassController's private utility methods via reflection.
 * These methods contain pure logic (no JavaFX dependency).
 */
class ClassControllerLogicTest {

    private ClassController controller;
    private Method normalizeMaterialType;
    private Method localizeMaterialType;
    private Method toApiMaterialType;
    private Method extractApiMessage;
    private Method getStringOrDefault;

    @BeforeEach
    void setUp() throws Exception {
        controller = new ClassController();

        normalizeMaterialType = ClassController.class.getDeclaredMethod("normalizeMaterialType", String.class);
        normalizeMaterialType.setAccessible(true);

        localizeMaterialType = ClassController.class.getDeclaredMethod("localizeMaterialType", String.class);
        localizeMaterialType.setAccessible(true);

        toApiMaterialType = ClassController.class.getDeclaredMethod("toApiMaterialType", String.class);
        toApiMaterialType.setAccessible(true);

        extractApiMessage = ClassController.class.getDeclaredMethod("extractApiMessage", String.class, String.class);
        extractApiMessage.setAccessible(true);

        getStringOrDefault = ClassController.class.getDeclaredMethod("getStringOrDefault", JsonObject.class, String.class, String.class);
        getStringOrDefault.setAccessible(true);
    }

    // ---- normalizeMaterialType ----

    @Test
    void normalizeMaterialTypeLectureNotes() throws Exception {
        assertEquals("lecture notes", normalizeMaterialType.invoke(controller, "Lecture Notes"));
    }

    @Test
    void normalizeMaterialTypeLectureNotesVariant() throws Exception {
        assertEquals("lecture notes", normalizeMaterialType.invoke(controller, "lecture_notes"));
    }

    @Test
    void normalizeMaterialTypeLectureNotesHyphen() throws Exception {
        assertEquals("lecture notes", normalizeMaterialType.invoke(controller, "Lecture-Notes"));
    }

    @Test
    void normalizeMaterialTypeAssignment() throws Exception {
        assertEquals("assignment", normalizeMaterialType.invoke(controller, "Assignment"));
    }

    @Test
    void normalizeMaterialTypeAssignmentUpper() throws Exception {
        assertEquals("assignment", normalizeMaterialType.invoke(controller, "ASSIGNMENT"));
    }

    @Test
    void normalizeMaterialTypeSlides() throws Exception {
        assertEquals("slides", normalizeMaterialType.invoke(controller, "Slides"));
    }

    @Test
    void normalizeMaterialTypeSlidesLower() throws Exception {
        assertEquals("slides", normalizeMaterialType.invoke(controller, "slides"));
    }

    @Test
    void normalizeMaterialTypeReference() throws Exception {
        assertEquals("reference", normalizeMaterialType.invoke(controller, "Reference"));
    }

    @Test
    void normalizeMaterialTypeReferenceMixed() throws Exception {
        assertEquals("reference", normalizeMaterialType.invoke(controller, "Reference Material"));
    }

    @Test
    void normalizeMaterialTypeOther() throws Exception {
        assertEquals("other", normalizeMaterialType.invoke(controller, "RandomType"));
    }

    @Test
    void normalizeMaterialTypeNull() throws Exception {
        assertEquals("other", normalizeMaterialType.invoke(controller, (String) null));
    }

    @Test
    void normalizeMaterialTypeBlank() throws Exception {
        assertEquals("other", normalizeMaterialType.invoke(controller, "   "));
    }

    @Test
    void normalizeMaterialTypeEmpty() throws Exception {
        assertEquals("other", normalizeMaterialType.invoke(controller, ""));
    }

    // ---- toApiMaterialType ----

    @Test
    void toApiMaterialTypeLectureNotes() throws Exception {
        assertEquals("Lecture Notes", toApiMaterialType.invoke(controller, "lecture_notes"));
    }

    @Test
    void toApiMaterialTypeAssignment() throws Exception {
        assertEquals("Assignment", toApiMaterialType.invoke(controller, "Assignment"));
    }

    @Test
    void toApiMaterialTypeSlides() throws Exception {
        assertEquals("Slides", toApiMaterialType.invoke(controller, "slides"));
    }

    @Test
    void toApiMaterialTypeReference() throws Exception {
        assertEquals("Reference", toApiMaterialType.invoke(controller, "Reference"));
    }

    @Test
    void toApiMaterialTypeOther() throws Exception {
        assertEquals("Other", toApiMaterialType.invoke(controller, "Unknown"));
    }

    @Test
    void toApiMaterialTypeNull() throws Exception {
        assertEquals("Other", toApiMaterialType.invoke(controller, (String) null));
    }

    // ---- localizeMaterialType ----

    @Test
    void localizeMaterialTypeLectureNotesReturnsNonNull() throws Exception {
        Object result = localizeMaterialType.invoke(controller, "Lecture Notes");
        assertNotNull(result);
    }

    @Test
    void localizeMaterialTypeAssignmentReturnsNonNull() throws Exception {
        Object result = localizeMaterialType.invoke(controller, "Assignment");
        assertNotNull(result);
    }

    @Test
    void localizeMaterialTypeSlidesReturnsNonNull() throws Exception {
        Object result = localizeMaterialType.invoke(controller, "slides");
        assertNotNull(result);
    }

    @Test
    void localizeMaterialTypeReferenceReturnsNonNull() throws Exception {
        Object result = localizeMaterialType.invoke(controller, "Reference");
        assertNotNull(result);
    }

    @Test
    void localizeMaterialTypeOtherReturnsNonNull() throws Exception {
        Object result = localizeMaterialType.invoke(controller, "xyz");
        assertNotNull(result);
    }

    // ---- extractApiMessage ----

    @Test
    void extractApiMessageValidJsonReturnsMessage() throws Exception {
        String json = "{\"message\":\"Course not found\"}";
        assertEquals("Course not found", extractApiMessage.invoke(controller, json, "fallback"));
    }

    @Test
    void extractApiMessageNoMessageFieldReturnsFallback() throws Exception {
        String json = "{\"error\":\"something\"}";
        assertEquals("fallback", extractApiMessage.invoke(controller, json, "fallback"));
    }

    @Test
    void extractApiMessageNullMessageFieldReturnsFallback() throws Exception {
        String json = "{\"message\":null}";
        assertEquals("fallback", extractApiMessage.invoke(controller, json, "fallback"));
    }

    @Test
    void extractApiMessageInvalidJsonReturnsFallback() throws Exception {
        assertEquals("fallback", extractApiMessage.invoke(controller, "not json", "fallback"));
    }

    @Test
    void extractApiMessageEmptyJsonReturnsFallback() throws Exception {
        assertEquals("fallback", extractApiMessage.invoke(controller, "{}", "fallback"));
    }

    @Test
    void extractApiMessageJsonArrayReturnsFallback() throws Exception {
        assertEquals("fallback", extractApiMessage.invoke(controller, "[1,2,3]", "fallback"));
    }

    @Test
    void extractApiMessageEmptyStringReturnsFallback() throws Exception {
        assertEquals("fallback", extractApiMessage.invoke(controller, "", "fallback"));
    }

    // ---- getStringOrDefault ----

    @Test
    void getStringOrDefaultFieldPresentReturnsValue() throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", "Test");
        assertEquals("Test", getStringOrDefault.invoke(controller, obj, "name", "default"));
    }

    @Test
    void getStringOrDefaultFieldMissingReturnsFallback() throws Exception {
        JsonObject obj = new JsonObject();
        assertEquals("default", getStringOrDefault.invoke(controller, obj, "name", "default"));
    }

    @Test
    void getStringOrDefaultFieldNullReturnsFallback() throws Exception {
        JsonObject obj = new JsonObject();
        obj.add("name", JsonNull.INSTANCE);
        assertEquals("default", getStringOrDefault.invoke(controller, obj, "name", "default"));
    }

    @Test
    void getStringOrDefaultFieldEmptyReturnsEmpty() throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", "");
        assertEquals("", getStringOrDefault.invoke(controller, obj, "name", "default"));
    }

    @Test
    void getStringOrDefaultDifferentFieldTypes() throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("count", 42);
        // getAsString works on number properties in Gson
        assertEquals("42", getStringOrDefault.invoke(controller, obj, "count", "default"));
    }
}
