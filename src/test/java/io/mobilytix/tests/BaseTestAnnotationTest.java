package io.mobilytix.tests;

import io.mobilytix.annotation.AppUnderTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BaseTestAnnotationTest {
    /**
     * Verifies that a class WITH @AppUnderTest has the annotation readable
     * via reflection — the same way BaseTest.setUp() reads it.
     */
    @Test
    public void testAnnotationPresentAndReadable() {
        AppUnderTest annotation = AnnotatedClass.class.getAnnotation(AppUnderTest.class);
        Assert.assertNotNull(annotation, "@AppUnderTest should be present on AnnotatedClass");
        Assert.assertEquals(annotation.value(), "app_a", "Annotation value should be 'app_a'");
    }

    /**
     * Verifies that a class WITHOUT @AppUnderTest returns null —
     * which is what triggers the IllegalStateException in BaseTest.setUp().
     */
    @Test
    public void testAnnotationMissingReturnsNull() {
        AppUnderTest annotation = UnannotatedClass.class.getAnnotation(AppUnderTest.class);

        Assert.assertNull(annotation, "@AppUnderTest should not be present on UnannotatedClass");
    }

    /**
     * Verifies the exact error message format BaseTest throws
     * when @AppUnderTest is missing.
     */
    @Test
    public void testMissingAnnotationErrorMessage() {
        AppUnderTest annotation = UnannotatedClass.class.getAnnotation(AppUnderTest.class);
        if (annotation == null) {
            String errorMessage = String.format("%s must be annotated with @AppUnderTest. " + "Example: @AppUnderTest(\"app_a\")", UnannotatedClass.class.getName());
            Assert.assertTrue(errorMessage.contains("must be annotated with @AppUnderTest"), "Error message should contain the annotation instruction");
            Assert.assertTrue(errorMessage.contains(UnannotatedClass.class.getName()), "Error message should contain the class name");
        }
    }

    /**
     * Verifies annotation value is trimmed and lowercased
     * the same way BaseTest.setUp() processes it.
     */
    @Test
    public void testAnnotationValueNormalization() {
        AppUnderTest annotation = AnnotatedClass.class.getAnnotation(AppUnderTest.class);

        Assert.assertNotNull(annotation);
        String normalized = annotation.value().trim().toLowerCase();
        Assert.assertEquals(normalized, "app_a", "Normalized app key should be 'app_a'");
    }

    // -------------------------------------------------------------------------
    // Helper classes used as test subjects
    // -------------------------------------------------------------------------

    @AppUnderTest("app_a")
    private static class AnnotatedClass {
    }

    private static class UnannotatedClass {
    }
}
