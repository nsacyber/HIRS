package hirs.attestationca.portal.page.utils;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.portal.datatables.Column;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Utility class for the Page Controller classes.
 */
@Log4j2
public final class ControllerPagesUtils {
    /**
     * This private constructor was created to silence checkstyle error.
     */
    private ControllerPagesUtils() {
    }

    /**
     * Helper method that returns a set of the specified class' non-static declared field names.
     *
     * @param specifiedClass specified Java class
     * @return set of non-static declared field names
     */
    public static Set<String> getNonStaticFieldNames(final Class<?> specifiedClass) {
        Set<String> fieldNames = new HashSet<>();
        Class<?> currentClass = specifiedClass;

        // In order to grab all the specified class' field names,
        // we will want to "climb the inheritance ladder" and grab the
        // specified class' super class and its ancestors' field names. We will continue going up the ladder
        // until there are no more superclasses left.
        while (currentClass != null) {
            // grab the current class' non-static declared field names
            Stream.of(currentClass.getDeclaredFields())
                    .filter(eachField -> !Modifier.isStatic(eachField.getModifiers()))
                    .map(Field::getName)
                    .forEach(fieldNames::add);

            // grab the current class' superclass
            currentClass = currentClass.getSuperclass();
        }

        return fieldNames;
    }

    /**
     * Helper method that attempts to return a list of searchable column names that
     * matches the names of the provided class' non-static declared fields.
     *
     * @param pageControllerClass the controller's entity class
     * @param columns             table columns
     * @return set of searchable column names
     */
    public static Set<String> findSearchableColumnsNames(
            final Class<?> pageControllerClass,
            final List<Column> columns) {
        // grab all the provided class' non-static declared fields
        Set<String> nonStaticFields = getNonStaticFieldNames(pageControllerClass);

        // grab the list of column names that are searchable
        List<String> searchableColumnNames = new ArrayList<>(columns.stream()
                .filter(Column::isSearchable)
                .map(Column::getName)
                .toList());

        Set<String> validSearchableColumnNames = new HashSet<>();

        // since platform chain type is not a column on the platform credential page,
        // but it is a field that is represented by the credential type column in the platform credential page
        // we want to include that as one of the searchable column names
        if (PlatformCredential.class.isAssignableFrom(pageControllerClass)) {
            searchableColumnNames.add("platformChainType");
        }

        // loop through the provided searchable column names
        for (String columnName : searchableColumnNames) {
            // if the column name is a nested field (usually indicated by periods)
            if (columnName.contains(".")) {
                // add the column name to the set
                validSearchableColumnNames.add(columnName);
                continue;
            }
            // loop through the non-static field names
            for (String nonStaticField : nonStaticFields) {
                // if there is a match between the column name and the non-static field
                if (columnName.equalsIgnoreCase(nonStaticField)) {
                    // add the non-static field name to the set
                    validSearchableColumnNames.add(nonStaticField);
                    break;
                }
            }
        }

        return validSearchableColumnNames;
    }

    /**
     * Helper method that converts any kind of certificate into a PEM string.
     *
     * @param certificate certificate
     * @return PEM string of certificate
     */
    public static String convertCertificateToPem(final Certificate certificate) {
        return "-----BEGIN CERTIFICATE-----\n"
                + Base64.getEncoder()
                .encodeToString(certificate.getRawBytes())
                + "\n-----END CERTIFICATE-----\n";
    }

    /**
     * Helper method that converts an array of certificates (presumably a trust chain of certificates)
     * into a PEM string.
     *
     * @param certificates array of certificates
     * @return PEM string of array of certificates
     */
    public static String convertCertificateArrayToPem(final Certificate[] certificates) {
        StringBuilder trustChainPEM = new StringBuilder();
        for (Certificate certificate : certificates) {
            trustChainPEM.append(convertCertificateToPem(certificate));
        }
        return trustChainPEM.toString();
    }

    /**
     * Helper method that manually paginates a generic list when
     * working outside the database or repository layer.
     *
     * @param list     generic list
     * @param pageable pageable
     * @param <T>      generic class
     * @return paginated sublist
     */
    public static <T> List<T> getPaginatedSubList(final List<T> list, final Pageable pageable) {
        final int fromIndex = (int) pageable.getOffset();
        final int toIndex = Math.min(fromIndex + pageable.getPageSize(), list.size());
        return list.subList(fromIndex, toIndex);
    }

}
