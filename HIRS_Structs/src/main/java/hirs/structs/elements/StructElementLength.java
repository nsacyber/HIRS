package hirs.structs.elements;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates that the annotated field value represents the length of another field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StructElementLength {

    /**
     * @return the field that this length represents.
     */
    String fieldName();
}
