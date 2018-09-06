package hirs.structs.elements;

import java.io.Serializable;

/**
 * Marker interface that identifies a Java Bean as a Struct and can be converted. Implementations of
 * this interface should annotate the classes type with  a {@link StructElements}. The fields can be
 * in any order, but should be in the correct order within the order annotation. It is required that
 * all {@link Struct} follow the standard Java Bean convention. That is, they must have a default
 * constructor as well as a getter / setter for each field defined in the  {@link StructElements}
 * annotation.
 *
 * @see StructElements
 * @see StructElementLength
 */
public interface Struct extends Serializable {
}
