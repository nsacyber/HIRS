package hirs.attestationca.portal.page.utils;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.service.util.DataTablesColumn;
import hirs.attestationca.portal.datatables.Column;
import hirs.attestationca.portal.datatables.Order;
import io.micrometer.common.util.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for the Page Controller classes.
 */
public final class ControllerPagesUtils {
    /**
     * This private constructor was created to silence checkstyle error.
     */
    private ControllerPagesUtils() {
    }

    /**
     * Helper method that creates a {@link Pageable} object for paginating and optionally sorting a dataset
     * based on DataTables input.
     * <p>
     * This method calculates the current page and page size, then constructs a {@link Pageable} object that
     * includes both pagination and sorting (if applicable) based on the provided inputs.
     * If no ordering is provided, the method will return a pageable object with just the default pagination
     * settings.
     *
     * @param pageStart   The starting index of the data from the DataTables input (typically for pagination).
     * @param pageEnd     The number of items to display per page (page size) from the DataTables input.
     * @param orderColumn An {@link Order} object containing the column name and direction for sorting
     * @return A {@link Pageable} object containing the pagination and sorting configuration.
     */
    public static Pageable createPageableObject(final int pageStart,
                                                final int pageEnd,
                                                final Order orderColumn) {
        // Calculate the current page number based on the starting index and page size
        final int currentPage = pageStart / pageEnd;

        // If pageSize is -1 (Show All), set a very large page size, otherwise keep the original page size
        final int pageSize = pageEnd != -1 ? pageEnd : Integer.MAX_VALUE;

        if (orderColumn != null) {
            // Convert the direction string to a Sort.Direction enum (ascending or descending)
            Sort.Direction sortDirection =
                    "desc".equalsIgnoreCase(orderColumn.getDir()) ? Sort.Direction.DESC : Sort.Direction.ASC;

            // Create a Sort object based on the column name and direction
            Sort sort = Sort.by(new Sort.Order(sortDirection, orderColumn.getName()));

            // Create the Pageable object with sorting
            return PageRequest.of(currentPage, pageSize, sort);
        }

        // Create the Pageable object without sorting if no order column is provided
        return PageRequest.of(currentPage, pageSize);
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
     * Helper method that attempts to return a set of searchable columns that has a user-defined search value
     * and logical operator applied to them.
     *
     * @param columns table columns
     * @return searchable columns that have a search criteria
     */
    public static Set<DataTablesColumn> findColumnsWithSearchCriteriaForColumnSpecificSearch(
            final List<Column> columns) {
        // Identify and return columns that are searchable and have both a user-defined search value
        // and a logical condition (e.g., "equals", "greater than", etc.) applied through column controls
        return columns.stream()
                // Filter to include only columns that are searchable
                .filter(Column::isSearchable)

                // Filter to include columns with non-null column controls that come with a column control
                // logic operator
                .filter(eachColumn -> eachColumn.getColumnControl() != null
                        && !StringUtils.isBlank(eachColumn.getColumnControl().getSearch().getLogic()))

                // Filter to include columns with both a non-empty search value and a defined logic operator
                // the search value can be empty if the logic value is empty or not empty
                .filter(eachColumn -> {
                    final String columnSearchLogic = eachColumn.getColumnControl().getSearch().getLogic();

                    // return true, if the logic operator is empty or not empty or if the search value is not
                    // null or blank. Otherwise, return false
                    return columnSearchLogic.equalsIgnoreCase("empty")
                            || columnSearchLogic.equalsIgnoreCase("notEmpty")
                            || !StringUtils.isBlank(eachColumn.getColumnControl().getSearch().getValue());
                })

                // Create an object that contains the column name, column specific search value,
                // logic value and search type
                .map(eachColumn -> DataTablesColumn.builder()
                        .columnName(eachColumn.getName())
                        .columnSearchLogic(eachColumn.getColumnControl().getSearch().getLogic())
                        .columnSearchType(eachColumn.getColumnControl().getSearch().getType())
                        .columnSearchTerm(eachColumn.getColumnControl().getSearch().getValue())
                        .build()
                ).collect(Collectors.toSet());
    }

    /**
     * Helper method that attempts to return a list of searchable column names that
     * matches the names of the provided class' non-static declared fields.
     *
     * @param pageControllerClass the controller's entity class
     * @param columns             table columns
     * @return set of searchable column names
     */
    public static Set<String> findSearchableColumnNamesForGlobalSearch(
            final Class<?> pageControllerClass,
            final List<Column> columns) {
        // grab all the provided class' non-static declared fields
        Set<String> nonStaticFields = getNonStaticFieldNames(pageControllerClass);

        List<String> searchableColumnNames = new ArrayList<>(columns.stream()
                .filter(Column::isSearchable) // filter out columns that are searchable
                .map(Column::getName) // grab each column's name
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
}
