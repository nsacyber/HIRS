package hirs.utils;

import org.apache.logging.log4j.Logger;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyString;
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
        Assert.assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME).get(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a value can be passed through with no actual validation.
     */
    @Test
    public void testNoValidationOnNull() {
        Assert.assertNull(
                StringValidator.check(null, FIELD_NAME).get()
        );
    }

    /**
     * Tests that a non-null check against a String passes validation.
     */
    @Test
    public void testValidateNotNullSuccess() {
        Assert.assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME).notNull().get(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a non-null check against a null value fails validation.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateNotNullFailOnNull() {
        StringValidator.check(null, FIELD_NAME).notNull();
    }

    /**
     * Tests that a not-blank check against a String passes validation.
     */
    @Test
    public void testValidateNotBlankSuccess() {
        Assert.assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME).notBlank().get(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a not-blank check fails against a null String.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateNotBlankFailOnNull() {
        StringValidator.check(null, FIELD_NAME).notBlank();
    }

    /**
     * Tests that a not-blank check fails against an empty String.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateNotBlankFailOnEmpty() {
        StringValidator.check("", FIELD_NAME).notBlank();
    }

    /**
     * Tests that a max-length check against a String passes validation.
     */
    @Test
    public void testValidateMaxLengthSuccess() {
        Assert.assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME).maxLength(LENGTH).get(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a max-length check against a null String passes validation.
     */
    @Test
    public void testValidateMaxLengthSuccessOnNull() {
        Assert.assertNull(
                StringValidator.check(null, FIELD_NAME).maxLength(LENGTH).get()
        );
    }

    /**
     * Tests that a max-length check against a String (which is too long) fails validation.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateMaxLengthFailOnLongString() {
        Assert.assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME).maxLength(SMALL_LENGTH).get(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a String which passes multiple validations successfully returns its value.
     */
    @Test
    public void testValidateSeveralConditionsSuccess() {
        Assert.assertEquals(
                StringValidator.check(NONEMPTY_VALUE, FIELD_NAME)
                        .notNull().notBlank().maxLength(LENGTH).get(),
                NONEMPTY_VALUE
        );
    }

    /**
     * Tests that a String can pass multiple validations and still fail the last one.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateSeveralConditionsFailOnLast() {
        StringValidator.check(NONEMPTY_VALUE, FIELD_NAME)
                .notNull().notBlank().maxLength(SMALL_LENGTH);
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
        Mockito.verify(mockLogger, times(1)).error(anyString());
    }
}
