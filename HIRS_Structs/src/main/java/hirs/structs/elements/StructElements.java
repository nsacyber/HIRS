package hirs.structs.elements;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to describe the ordering of elements to be processed.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StructElements {

    /**
     * elements in order to be processed by a converter.
     */
    String[] elements();
}
