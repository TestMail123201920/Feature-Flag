package com.company.featureflag.rule.domain;

import com.company.featureflag.configuration.domain.ConditionConfig;
import com.company.featureflag.evaluation.domain.EvaluationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Evaluates a single {@link ConditionConfig} against an {@link EvaluationContext}.
 * Operates on the wire-format config DTO (not the JPA entity) so this exact
 * evaluation logic can later be reused verbatim by the SDK's local
 * evaluation engine (spec §28: "do not duplicate evaluation logic between
 * the SDK and central service").
 *
 * Data-driven by design: operator behavior lives in a lookup table keyed by
 * {@link Operator}, so a new operator is added by adding a table entry, not
 * by touching control flow or adding a new class per operator (spec §60:
 * do not overengineer).
 */
@Component
public class ConditionEvaluator {

    private final Map<Operator, BiPredicate<Object, Object>> operators = new EnumMap<>(Operator.class);

    public ConditionEvaluator() {
        operators.put(Operator.EQUALS, this::equalsValue);
        operators.put(Operator.NOT_EQUALS, (actual, expected) -> !equalsValue(actual, expected));
        operators.put(Operator.IN, (actual, expected) -> toCollection(expected).stream()
                .anyMatch(e -> equalsValue(actual, e)));
        operators.put(Operator.NOT_IN, (actual, expected) -> toCollection(expected).stream()
                .noneMatch(e -> equalsValue(actual, e)));
        operators.put(Operator.CONTAINS, (actual, expected) -> asString(actual).contains(asString(expected)));
        operators.put(Operator.STARTS_WITH, (actual, expected) -> asString(actual).startsWith(asString(expected)));
        operators.put(Operator.ENDS_WITH, (actual, expected) -> asString(actual).endsWith(asString(expected)));
        operators.put(Operator.GREATER_THAN, (actual, expected) -> compareNumeric(actual, expected) > 0);
        operators.put(Operator.GREATER_THAN_OR_EQUAL, (actual, expected) -> compareNumeric(actual, expected) >= 0);
        operators.put(Operator.LESS_THAN, (actual, expected) -> compareNumeric(actual, expected) < 0);
        operators.put(Operator.LESS_THAN_OR_EQUAL, (actual, expected) -> compareNumeric(actual, expected) <= 0);
        // EXISTS / NOT_EXISTS are handled in evaluate() before dispatch, since
        // they don't need an "actual" value at all.
    }

    public boolean evaluate(ConditionConfig condition, EvaluationContext context) {
        Operator operator = Operator.valueOf(condition.operator());

        if (operator == Operator.EXISTS) {
            return context.has(condition.field());
        }
        if (operator == Operator.NOT_EXISTS) {
            return !context.has(condition.field());
        }

        Object actual = context.get(condition.field()).orElse(null);
        if (actual == null) {
            return false; // a missing field never matches a value-comparison operator
        }

        BiPredicate<Object, Object> predicate = operators.get(operator);
        if (predicate == null) {
            throw new IllegalStateException("Unsupported operator: " + operator);
        }
        return predicate.test(actual, condition.value());
    }

    private boolean equalsValue(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            return compareNumeric(actual, expected) == 0;
        }
        return asString(actual).equals(asString(expected));
    }

    private int compareNumeric(Object actual, Object expected) {
        return new BigDecimal(asString(actual)).compareTo(new BigDecimal(asString(expected)));
    }

    private String asString(Object value) {
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> toCollection(Object expected) {
        if (expected instanceof Collection<?> collection) {
            return (Collection<Object>) collection;
        }
        throw new IllegalArgumentException("IN/NOT_IN operator requires a list value");
    }
}
