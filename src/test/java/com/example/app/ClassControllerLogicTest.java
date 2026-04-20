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
    void normalizeMaterialType_lectureNotes() throws Exception {
        assertEquals("lecture notes", normalizeMaterialType.invoke(controller, "Lecture Notes"));
    }

    @Test
    void normalizeMaterialType_lectureNotesVariant() throws Exception {
        assertEquals("lecture notes", normalizeMaterialType.invoke(controller, "lecture_notes"));
    }

    @Test
    void normalizeMaterialType_lectureNotesHyphen() throws Exception {
        assertEquals("lecture notes", normalizeMaterialType.invoke(controller, "Lecture-Notes"));
    }

    @Test
    void normalizeMaterialType_assignment() throws Exception {
        assertEquals("assignment", normalizeMaterialType.invoke(controller, "Assignment"));
    }

    @Test
    void normalizeMaterialType_assignmentUpper() throws Exception {
        assertEquals("assignment", normalizeMaterialType.invoke(controller, "ASSIGNMENT"));
    }

    @Test
    void normalizeMaterialType_slides() throws Exception {
        assertEquals("slides", normalizeMaterialType.invoke(controller, "Slides"));
    }

    @Test
    void normalizeMaterialType_slidesLower() throws Exception {
        assertEquals("slides", normalizeMaterialType.invoke(controller, "slides"));
    }

    @Test
    void normalizeMaterialType_reference() throws Exception {
        assertEquals("reference", normalizeMaterialType.invoke(controller, "Reference"));
    }

    @Test
    void normalizeMaterialType_referenceMixed() throws Exception {
        assertEquals("reference", normalizeMaterialType.invoke(controller, "Reference Material"));
    }

    @Test
    void normalizeMaterialType_other() throws Exception {
        assertEquals("other", normalizeMaterialType.invoke(controller, "RandomType"));
    }

    @Test
    void normalizeMaterialType_null() throws Exception {
        assertEquals("other", normalizeMaterialType.invoke(controller, (String) null));
    }

    @Test
    void normalizeMaterialType_blank() throws Exception {
        assertEquals("other", normalizeMaterialType.invoke(controller, "   "));
    }

    @Test
    void normalizeMaterialType_empty() throws Exception {
        assertEquals("other", normalizeMaterialType.invoke(controller, ""));
    }

    // ---- toApiMaterialType ----

    @Test
    void toApiMaterialType_lectureNotes() throws Exception {
        assertEquals("Lecture Notes", toApiMaterialType.invoke(controller, "lecture_notes"));
    }

    @Test
    void toApiMaterialType_assignment() throws Exception {
        assertEquals("Assignment", toApiMaterialType.invoke(controller, "Assignment"));
    }

    @Test
    void toApiMaterialType_slides() throws Exception {
        assertEquals("Slides", toApiMaterialType.invoke(controller, "slides"));
    }

    @Test
    void toApiMaterialType_reference() throws Exception {
        assertEquals("Reference", toApiMaterialType.invoke(controller, "Reference"));
    }

    @Test
    void toApiMaterialType_other() throws Exception {
        assertEquals("Other", toApiMaterialType.invoke(controller, "Unknown"));
    }

    @Test
    void toApiMaterialType_null() throws Exception {
        assertEquals("Other", toApiMaterialType.invoke(controller, (String) null));
    }

    // ---- localizeMaterialType ----

    @Test
    void localizeMaterialType_lectureNotes_returnsNonNull() throws Exception {
        Object result = localizeMaterialType.invoke(controller, "Lecture Notes");
        assertNotNull(result);
    }

    @Test
    void localizeMaterialType_assignment_returnsNonNull() throws Exception {
        Object result = localizeMaterialType.invoke(controller, "Assignment");
        assertNotNull(result);
    }

    @Test
    void localizeMaterialType_slides_returnsNonNull() throws Exception {
        Object result = localizeMaterialType.invoke(controller, "slides");
        assertNotNull(result);
    }

    @Test
    void localizeMaterialType_reference_returnsNonNull() throws Exception {
        Object result = localizeMaterialType.invoke(controller, "Reference");
        assertNotNull(result);
    }

    @Test
    void localizeMaterialType_other_returnsNonNull() throws Exception {
        Object result = localizeMaterialType.invoke(controller, "xyz");
        assertNotNull(result);
    }

    // ---- extractApiMessage ----

    @Test
    void extractApiMessage_validJson_returnsMessage() throws Exception {
        String json = "{\"message\":\"Course not found\"}";
        assertEquals("Course not found", extractApiMessage.invoke(controller, json, "fallback"));
    }

    @Test
    void extractApiMessage_noMessageField_returnsFallback() throws Exception {
        String json = "{\"error\":\"something\"}";
        assertEquals("fallback", extractApiMessage.invoke(controller, json, "fallback"));
    }

    @Test
    void extractApiMessage_nullMessageField_returnsFallback() throws Exception {
        String json = "{\"message\":null}";
        assertEquals("fallback", extractApiMessage.invoke(controller, json, "fallback"));
    }

    @Test
    void extractApiMessage_invalidJson_returnsFallback() throws Exception {
        assertEquals("fallback", extractApiMessage.invoke(controller, "not json", "fallback"));
    }

    @Test
    void extractApiMessage_emptyJson_returnsFallback() throws Exception {
        assertEquals("fallback", extractApiMessage.invoke(controller, "{}", "fallback"));
    }

    @Test
    void extractApiMessage_jsonArray_returnsFallback() throws Exception {
        assertEquals("fallback", extractApiMessage.invoke(controller, "[1,2,3]", "fallback"));
    }

    @Test
    void extractApiMessage_emptyString_returnsFallback() throws Exception {
        assertEquals("fallback", extractApiMessage.invoke(controller, "", "fallback"));
    }

    // ---- getStringOrDefault ----

    @Test
    void getStringOrDefault_fieldPresent_returnsValue() throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", "Test");
        assertEquals("Test", getStringOrDefault.invoke(controller, obj, "name", "default"));
    }

    @Test
    void getStringOrDefault_fieldMissing_returnsFallback() throws Exception {
        JsonObject obj = new JsonObject();
        assertEquals("default", getStringOrDefault.invoke(controller, obj, "name", "default"));
    }

    @Test
    void getStringOrDefault_fieldNull_returnsFallback() throws Exception {
        JsonObject obj = new JsonObject();
        obj.add("name", JsonNull.INSTANCE);
        assertEquals("default", getStringOrDefault.invoke(controller, obj, "name", "default"));
    }

    @Test
    void getStringOrDefault_fieldEmpty_returnsEmpty() throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", "");
        assertEquals("", getStringOrDefault.invoke(controller, obj, "name", "default"));
    }

    @Test
    void getStringOrDefault_differentFieldTypes() throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("count", 42);
        // getAsString works on number properties in Gson
        assertEquals("42", getStringOrDefault.invoke(controller, obj, "count", "default"));
    }
}
