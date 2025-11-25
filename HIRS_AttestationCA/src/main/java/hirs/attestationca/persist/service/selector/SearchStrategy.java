package hirs.attestationca.persist.service.selector;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public interface SearchStrategy {
    Predicate apply(CriteriaBuilder criteriaBuilder, Path<?> fieldPath, String searchTerm);
}
