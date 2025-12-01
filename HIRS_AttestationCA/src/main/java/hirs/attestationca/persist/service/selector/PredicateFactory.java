package hirs.attestationca.persist.service.selector;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Date;

/**
 * A factory class responsible for creating various types of {@link Predicate} objects used for
 * filtering data in JPA Criteria queries.
 *
 * <p>This class provides utility methods to dynamically build predicates based on different search criteria
 * for both {@link String} and {@link Integer} fields. The predicates are used in JPA Criteria API to generate
 * SQL-based queries with conditions that are applied at runtime based on the user's input.</p>
 *
 * <p>The factory supports a wide range of common search operations such as:</p>
 * <ul>
 *     <li>String-based operations: equality, containment, starts/ends with, and
 *     emptiness checks</li>
 *     <li>Integer-based operations: equality, comparison (greater than, less than, etc.),
 *     and emptiness checks</li>
 * </ul>
 *
 * <p>Examples of supported search logics include:</p>
 * <ul>
 *     <li>For strings: "equals", "contains", "not empty", "empty", "starts", "ends"</li>
 *     <li>For integers: "equals", "greater", "less", "greater or equal", "less or equal", "not empty",
 *     "empty"</li>
 * </ul>
 */
public class PredicateFactory {

    /**
     * Creates a predicate based on the search logic
     *
     * @param criteriaBuilder  the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath        The path to the entity field
     * @param columnSearchTerm The search term to filter by
     * @param searchLogic      The search logic (e.g., "contains", "equals", etc.)
     * @return The corresponding Predicate based on the search logic
     */
    public static Predicate createPredicateForStringFields(final CriteriaBuilder criteriaBuilder,
                                                           final Path<String> fieldPath,
                                                           final String columnSearchTerm,
                                                           final String searchLogic) {
        return switch (searchLogic.toLowerCase()) {
            case "equal" -> buildEqualsPredicateForStringFields(criteriaBuilder, fieldPath, columnSearchTerm);
            case "contains" ->
                    buildContainsPredicateForStringFields(criteriaBuilder, fieldPath, columnSearchTerm);
            case "notcontains" ->
                    buildDoesNotContainPredicateForStringFields(criteriaBuilder, fieldPath, columnSearchTerm);
            case "notequal" ->
                    buildDoesNotEqualPredicateForStringFields(criteriaBuilder, fieldPath, columnSearchTerm);
            case "notempty" ->
                    PredicateFactory.buildNotEmptyPredicateForStringFields(criteriaBuilder, fieldPath);
            case "starts" ->
                    buildStartsWithPredicateForStringFields(criteriaBuilder, fieldPath, columnSearchTerm);
            case "ends" ->
                    buildEndsWithPredicateForStringFields(criteriaBuilder, fieldPath, columnSearchTerm);
            case "empty" -> PredicateFactory.buildEmptyPredicateForStringFields(criteriaBuilder, fieldPath);
            default -> throw new UnsupportedOperationException("Search logic not supported: " + searchLogic);
        };
    }

    /**
     * Creates a predicate based on the search logic and search term for integer fields.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the integer field to query (must be an {@link Integer})
     * @param searchTerm      the search term to compare against
     * @param searchLogic     the search logic to apply (e.g., "equals", "greater", "less", etc.)
     * @return a {@link Predicate} corresponding to the specified logic and search term
     */
    public static Predicate createPredicateForIntegerFields(final CriteriaBuilder criteriaBuilder,
                                                            final Path<Integer> fieldPath,
                                                            final Integer searchTerm,
                                                            final String searchLogic) {
        return switch (searchLogic.toLowerCase()) {
            case "equal" -> buildEqualsPredicateForIntegerFields(criteriaBuilder, fieldPath, searchTerm);
            case "notequal" ->
                    buildDoesNotEqualPredicateForIntegerFields(criteriaBuilder, fieldPath, searchTerm);
            case "greater" ->
                    buildGreaterThanPredicateForIntegerFields(criteriaBuilder, fieldPath, searchTerm);
            case "less" -> buildLessThanPredicateForIntegerFields(criteriaBuilder, fieldPath, searchTerm);
            case "greaterorequal" ->
                    buildGreaterThanOrEqualToPredicateForIntegerFields(criteriaBuilder, fieldPath,
                            searchTerm);
            case "lessorequal" ->
                    buildLessThanOrEqualToPredicateForIntegerFields(criteriaBuilder, fieldPath, searchTerm);
            case "notempty" -> buildNotEmptyPredicateForIntegerFields(criteriaBuilder, fieldPath);
            case "empty" -> buildEmptyPredicateForIntegerFields(criteriaBuilder, fieldPath);
            default -> throw new IllegalArgumentException("Invalid search logic: " + searchLogic);
        };
    }

    // String-based predicates

    /**
     * Builds a predicate that checks if the given field equals the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the field to compare (must be a {@link String})
     * @param searchTerm      the value to compare the field against (case-insensitive)
     * @return a {@link Predicate} that represents the equality check between the field and the search term
     */
    private static Predicate buildEqualsPredicateForStringFields(final CriteriaBuilder criteriaBuilder,
                                                                 final Path<String> fieldPath,
                                                                 String searchTerm) {
        return criteriaBuilder.equal(criteriaBuilder.lower(fieldPath), searchTerm.toLowerCase());
    }

    /**
     * Builds a predicate that checks if the given field contains the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the field to search within (must be a {@link String})
     * @param searchTerm      the value to search for within the field (case-insensitive)
     * @return a {@link Predicate} that checks if the field contains the search term
     */
    private static Predicate buildContainsPredicateForStringFields(final CriteriaBuilder criteriaBuilder,
                                                                   final Path<String> fieldPath,
                                                                   String searchTerm) {
        return criteriaBuilder.like(criteriaBuilder.lower(fieldPath), "%" + searchTerm.toLowerCase() + "%");
    }

    /**
     * Builds a predicate that checks if the given field does not contain the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the field to search within (must be a {@link String})
     * @param searchTerm      the value to ensure is not contained within the field (case-insensitive)
     * @return a {@link Predicate} that checks if the field does not contain the search term
     */
    private static Predicate buildDoesNotContainPredicateForStringFields(
            final CriteriaBuilder criteriaBuilder,
            final Path<String> fieldPath,
            final String searchTerm) {
        return criteriaBuilder.notLike(criteriaBuilder.lower(fieldPath),
                "%" + searchTerm.toLowerCase() + "%");
    }

    /**
     * Builds a predicate that checks if the given field does not equal the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the field to compare (must be a {@link String})
     * @param searchTerm      the value to compare the field against (case-insensitive)
     * @return a {@link Predicate} that represents the inequality check between the field and the search term
     */
    private static Predicate buildDoesNotEqualPredicateForStringFields(final CriteriaBuilder criteriaBuilder,
                                                                       final Path<String> fieldPath,
                                                                       final String searchTerm) {
        return criteriaBuilder.notEqual(criteriaBuilder.lower(fieldPath), searchTerm.toLowerCase());
    }

    /**
     * Builds a predicate that checks if the given field is not empty. A field is considered not empty if it is
     * neither null nor an empty string.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the field to check (must be a {@link String})
     * @return a {@link Predicate} that ensures the field is not null and not an empty string
     */
    private static Predicate buildNotEmptyPredicateForStringFields(final CriteriaBuilder criteriaBuilder,
                                                                   final Path<String> fieldPath) {
        // Not empty means not null or not an empty string
        return criteriaBuilder.and(
                criteriaBuilder.isNotNull(fieldPath),
                criteriaBuilder.notEqual(fieldPath, "")
        );
    }

    /**
     * Builds a predicate that checks if the given field starts with the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the field to search within (must be a {@link String})
     * @param searchTerm      the value to check for at the beginning of the field (case-insensitive)
     * @return a {@link Predicate} that checks if the field starts with the search term
     */
    private static Predicate buildStartsWithPredicateForStringFields(final CriteriaBuilder criteriaBuilder,
                                                                     final Path<String> fieldPath,
                                                                     final String searchTerm) {
        return criteriaBuilder.like(criteriaBuilder.lower(fieldPath), searchTerm.toLowerCase() + "%");
    }

    /**
     * Builds a predicate that checks if the given field ends with the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the field to search within (must be a {@link String})
     * @param searchTerm      the value to check for at the end of the field (case-insensitive)
     * @return a {@link Predicate} that checks if the field ends with the search term
     */
    private static Predicate buildEndsWithPredicateForStringFields(final CriteriaBuilder criteriaBuilder,
                                                                   final Path<String> fieldPath,
                                                                   final String searchTerm) {
        return criteriaBuilder.like(criteriaBuilder.lower(fieldPath), "%" + searchTerm.toLowerCase());
    }

    /**
     * Builds a predicate that checks if the given field is empty. A field is considered empty if it is either null
     * or an empty string.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the field to check (must be a {@link String})
     * @return a {@link Predicate} that ensures the field is either null or an empty string
     */
    private static Predicate buildEmptyPredicateForStringFields(final CriteriaBuilder criteriaBuilder,
                                                                final Path<String> fieldPath) {
        // Empty means null or an empty string
        return criteriaBuilder.or(
                criteriaBuilder.isNull(fieldPath),
                criteriaBuilder.equal(fieldPath, "")
        );
    }

    // Integer-based predicates

    /**
     * Builds a predicate that checks if the given integer field equals the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the integer field to compare
     *                        (must be an {@link Integer})
     * @param searchTerm      the value to compare the field against
     * @return a {@link Predicate} that represents the equality check between the field and the search term
     */
    public static Predicate buildEqualsPredicateForIntegerFields(final CriteriaBuilder criteriaBuilder,
                                                                 final Path<Integer> fieldPath,
                                                                 final Integer searchTerm) {
        return criteriaBuilder.equal(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate that checks if the given integer field does not equal the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the integer field to compare
     *                        (must be an {@link Integer})
     * @param searchTerm      the value to compare the field against
     * @return a {@link Predicate} that represents the inequality check between the field and the search term
     */
    public static Predicate buildDoesNotEqualPredicateForIntegerFields(final CriteriaBuilder criteriaBuilder,
                                                                       final Path<Integer> fieldPath,
                                                                       final Integer searchTerm) {
        return criteriaBuilder.notEqual(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate that checks if the given integer field is greater than the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the integer field to compare
     *                        (must be an {@link Integer})
     * @param searchTerm      the value to compare the field against
     * @return a {@link Predicate} that checks if the field is greater than the search term
     */
    public static Predicate buildGreaterThanPredicateForIntegerFields(final CriteriaBuilder criteriaBuilder,
                                                                      final Path<Integer> fieldPath,
                                                                      final Integer searchTerm) {
        return criteriaBuilder.greaterThan(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate that checks if the given integer field is less than the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the integer field to compare
     *                        (must be an {@link Integer})
     * @param searchTerm      the value to compare the field against
     * @return a {@link Predicate} that checks if the field is less than the search term
     */
    public static Predicate buildLessThanPredicateForIntegerFields(final CriteriaBuilder criteriaBuilder,
                                                                   final Path<Integer> fieldPath,
                                                                   final Integer searchTerm) {
        return criteriaBuilder.lessThan(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate that checks if the given integer field is greater than or equal to the
     * specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the integer field to compare
     *                        (must be an {@link Integer})
     * @param searchTerm      the value to compare the field against
     * @return a {@link Predicate} that checks if the field is greater than or equal to the search term
     */
    public static Predicate buildGreaterThanOrEqualToPredicateForIntegerFields(
            final CriteriaBuilder criteriaBuilder,
            final Path<Integer> fieldPath,
            final Integer searchTerm) {
        return criteriaBuilder.greaterThanOrEqualTo(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate that checks if the given integer field is less than or equal to the specified search term.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the integer field to compare
     *                        (must be an {@link Integer})
     * @param searchTerm      the value to compare the field against
     * @return a {@link Predicate} that checks if the field is less than or equal to the search term
     */
    public static Predicate buildLessThanOrEqualToPredicateForIntegerFields(
            final CriteriaBuilder criteriaBuilder,
            final Path<Integer> fieldPath,
            final Integer searchTerm) {
        return criteriaBuilder.lessThanOrEqualTo(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate that checks if the given integer field is not empty. A field is considered not empty if it is
     * neither null nor zero.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the integer field to check
     *                        (must be an {@link Integer})
     * @return a {@link Predicate} that ensures the field is not null and not zero
     */
    public static Predicate buildNotEmptyPredicateForIntegerFields(final CriteriaBuilder criteriaBuilder,
                                                                   final Path<Integer> fieldPath) {
        return criteriaBuilder.and(
                criteriaBuilder.isNotNull(fieldPath),
                criteriaBuilder.notEqual(fieldPath, 0)
        );
    }

    /**
     * Builds a predicate that checks if the given integer field is empty. A field is considered
     * empty if it is either null or zero.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the integer field to check
     *                        (must be an {@link Integer})
     * @return a {@link Predicate} that ensures the field is either null or zero
     */
    public static Predicate buildEmptyPredicateForIntegerFields(final CriteriaBuilder criteriaBuilder,
                                                                final Path<Integer> fieldPath) {
        return criteriaBuilder.or(
                criteriaBuilder.isNull(fieldPath),
                criteriaBuilder.equal(fieldPath, 0)
        );
    }

    // Date-based fields

    /**
     * Creates a predicate based on the search logic and search term for Date fields.
     *
     * @param criteriaBuilder the {@link CriteriaBuilder} used to construct the predicate
     * @param fieldPath       the {@link Path} representing the Date field to query
     * @param searchTerm      the search term (of type {@link Date}) to compare against
     * @param searchLogic     the search logic to apply (e.g., "equals", "before", "after", etc.)
     * @return a {@link Predicate} corresponding to the specified logic and search term
     */
    public static Predicate createPredicateForDateFields(CriteriaBuilder criteriaBuilder,
                                                         Path<Date> fieldPath,
                                                         Date searchTerm, String searchLogic) {
        return switch (searchLogic.toLowerCase()) {
            case "equal" -> buildEqualsPredicateForDateFields(criteriaBuilder, fieldPath, searchTerm);
            case "notequal" ->
                    buildDoesNotEqualPredicateForDateFields(criteriaBuilder, fieldPath, searchTerm);
            case "before" -> buildBeforePredicateForDateFields(criteriaBuilder, fieldPath, searchTerm);
            case "after" -> buildAfterPredicateForDateFields(criteriaBuilder, fieldPath, searchTerm);
            case "onorbefore" ->
                    buildOnOrBeforePredicateForDateFields(criteriaBuilder, fieldPath, searchTerm);
            case "onorafter" -> buildOnOrAfterPredicateForDateFields(criteriaBuilder, fieldPath, searchTerm);
            case "notempty" -> buildNotEmptyPredicateForDateFields(criteriaBuilder, fieldPath);
            case "empty" -> buildEmptyPredicateForDateFields(criteriaBuilder, fieldPath);
            default -> throw new IllegalArgumentException("Invalid search logic: " + searchLogic);
        };
    }

    /**
     * Builds a predicate to check if a Date field is equal to a given search term.
     *
     * @param criteriaBuilder The CriteriaBuilder used to construct the query.
     * @param fieldPath       The path to the Date field in the entity.
     * @param searchTerm      The Date value to compare the field against.
     * @return A Predicate representing the equality check for the Date field.
     */
    private static Predicate buildEqualsPredicateForDateFields(final CriteriaBuilder criteriaBuilder,
                                                               final Path<Date> fieldPath,
                                                               final Date searchTerm) {
        return criteriaBuilder.equal(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate to check if a Date field is not equal to a given search term.
     *
     * @param criteriaBuilder The CriteriaBuilder used to construct the query.
     * @param fieldPath       The path to the Date field in the entity.
     * @param searchTerm      The Date value to compare the field against.
     * @return A Predicate representing the inequality check for the Date field.
     */
    private static Predicate buildDoesNotEqualPredicateForDateFields(final CriteriaBuilder criteriaBuilder,
                                                                     final Path<Date> fieldPath,
                                                                     final Date searchTerm) {
        return criteriaBuilder.notEqual(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate to check if a Date field is before a given search term.
     *
     * @param criteriaBuilder The CriteriaBuilder used to construct the query.
     * @param fieldPath       The path to the Date field in the entity.
     * @param searchTerm      The Date value to compare the field against.
     * @return A Predicate representing the "before" check for the Date field.
     */
    private static Predicate buildBeforePredicateForDateFields(final CriteriaBuilder criteriaBuilder,
                                                               final Path<Date> fieldPath,
                                                               final Date searchTerm) {
        return criteriaBuilder.lessThan(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate to check if a Date field is after a given search term.
     *
     * @param criteriaBuilder The CriteriaBuilder used to construct the query.
     * @param fieldPath       The path to the Date field in the entity.
     * @param searchTerm      The Date value to compare the field against.
     * @return A Predicate representing the "after" check for the Date field.
     */
    private static Predicate buildAfterPredicateForDateFields(final CriteriaBuilder criteriaBuilder,
                                                              final Path<Date> fieldPath,
                                                              final Date searchTerm) {
        return criteriaBuilder.greaterThan(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate to check if a Date field is on or before a given search term.
     *
     * @param criteriaBuilder The CriteriaBuilder used to construct the query.
     * @param fieldPath       The path to the Date field in the entity.
     * @param searchTerm      The Date value to compare the field against.
     * @return A Predicate representing the "on or before" check for the Date field.
     */
    private static Predicate buildOnOrBeforePredicateForDateFields(final CriteriaBuilder criteriaBuilder,
                                                                   final Path<Date> fieldPath,
                                                                   final Date searchTerm) {
        return criteriaBuilder.lessThanOrEqualTo(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate to check if a Date field is on or after a given search term.
     *
     * @param criteriaBuilder The CriteriaBuilder used to construct the query.
     * @param fieldPath       The path to the Date field in the entity.
     * @param searchTerm      The Date value to compare the field against.
     * @return A Predicate representing the "on or after" check for the Date field.
     */
    private static Predicate buildOnOrAfterPredicateForDateFields(final CriteriaBuilder criteriaBuilder,
                                                                  final Path<Date> fieldPath,
                                                                  final Date searchTerm) {
        return criteriaBuilder.greaterThanOrEqualTo(fieldPath, searchTerm);
    }

    /**
     * Builds a predicate to check if a Date field is not null.
     *
     * @param criteriaBuilder The CriteriaBuilder used to construct the query.
     * @param fieldPath       The path to the Date field in the entity.
     * @return A Predicate representing the "is not null" check for the Date field.
     */
    private static Predicate buildNotEmptyPredicateForDateFields(final CriteriaBuilder criteriaBuilder,
                                                                 final Path<Date> fieldPath) {
        return criteriaBuilder.isNotNull(fieldPath);
    }

    /**
     * Builds a predicate to check if a Date field is null.
     *
     * @param criteriaBuilder The CriteriaBuilder used to construct the query.
     * @param fieldPath       The path to the Date field in the entity.
     * @return A Predicate representing the "is null" check for the Date field.
     */
    private static Predicate buildEmptyPredicateForDateFields(final CriteriaBuilder criteriaBuilder,
                                                              final Path<Date> fieldPath) {
        return criteriaBuilder.isNull(fieldPath);
    }

}
