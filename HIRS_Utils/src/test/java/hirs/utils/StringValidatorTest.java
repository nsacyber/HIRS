package hirs.utils;

import org.apache.logging.log4j.Logger;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;

/**
 * Contains tests for StringValidator.
 */
public class StringValidatorTest {
    private static final int SMALL_LENGTH = 5;
    private static final int LENGTH = 32;
    private static final String NONEMPTY_VALUE = "test value";
    private static final String FIELD_NAME = "test field";

    /**
     * Tests that a value can be passed through with no actual validation.
     */
    @Test
    public void testNoValidation() {
        assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME).getValue(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a value can be passed through with no actual validation.
     */
    @Test
    public void testNoValidationOnNull() {
        assertNull(
                StringValidator.check(null, FIELD_NAME).getValue()
        );
    }

    /**
     * Tests that a non-null check against a String passes validation.
     */
    @Test
    public void testValidateNotNullSuccess() {
        assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME).notNull().getValue(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a non-null check against a null value fails validation.
     */
    @Test
    public void testValidateNotNullFailOnNull() {
        assertThrows(IllegalArgumentException.class, () ->
                StringValidator.check(null, FIELD_NAME).notNull());
    }

    /**
     * Tests that a not-blank check against a String passes validation.
     */
    @Test
    public void testValidateNotBlankSuccess() {
        assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME).notBlank().getValue(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a not-blank check fails against a null String.
     */
    @Test
    public void testValidateNotBlankFailOnNull() {
        assertThrows(IllegalArgumentException.class, () ->
                StringValidator.check(null, FIELD_NAME).notBlank());
    }

    /**
     * Tests that a not-blank check fails against an empty String.
     */
    @Test
    public void testValidateNotBlankFailOnEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                StringValidator.check("", FIELD_NAME).notBlank());
    }

    /**
     * Tests that a max-length check against a String passes validation.
     */
    @Test
    public void testValidateMaxLengthSuccess() {
        assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME).maxLength(LENGTH).getValue(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a max-length check against a null String passes validation.
     */
    @Test
    public void testValidateMaxLengthSuccessOnNull() {
        assertNull(
                StringValidator.check(null, FIELD_NAME).maxLength(LENGTH).getValue()
        );
    }

    /**
     * Tests that a max-length check against a String (which is too long) fails validation.
     */
    @Test
    public void testValidateMaxLengthFailOnLongString() {
        assertThrows(IllegalArgumentException.class, () ->
            assertEquals(
                    StringValidator.check(NONEMPTY_VALUE, FIELD_NAME).maxLength(SMALL_LENGTH).getValue(),
                    NONEMPTY_VALUE
            ));
    }

    /**
     * Tests that a String which passes multiple validations successfully returns its value.
     */
    @Test
    public void testValidateSeveralConditionsSuccess() {
        assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME)
                        .notNull().notBlank().maxLength(LENGTH).getValue(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a String can pass multiple validations and still fail the last one.
     */
    @Test
    public void testValidateSeveralConditionsFailOnLast() {
        assertThrows(IllegalArgumentException.class, () ->
            StringValidator.check(NONEMPTY_VALUE, FIELD_NAME)
                    .notNull().notBlank().maxLength(SMALL_LENGTH));
    }

    /**
     * Tests that StringValidator will log error output to a provided logger.
     */
    @Test
    public void testCheckLoggerIsUsedOnFailure() {
        Logger mockLogger = Mockito.mock(Logger.class);
        try {
            StringValidator.check(NONEMPTY_VALUE, FIELD_NAME, mockLogger).maxLength(SMALL_LENGTH);
        } catch (IllegalArgumentException e) {
            System.out.println("Avoiding empty catch block.");
        }
        Mockito.verify(mockLogger, times(1)).error(Mockito.anyString());
    }
}