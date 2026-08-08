package com.company.featureflag.sdk.evaluation;

import com.company.featureflag.sdk.config.ConditionConfig;
import com.company.featureflag.sdk.core.EvaluationContext;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Mirrors the central service's ConditionEvaluator exactly (same operator
 * set, same semantics) so a given (feature config, context) pair produces
 * the same decision whether evaluated here or server-side — spec §28's
 * "do not duplicate evaluation logic" applied as closely as two genuinely
 * separate Maven artifacts allow.
 */
final class ConditionEvaluator {

    private enum Operator {
        EQUALS, NOT_EQUALS, IN, NOT_IN, CONTAINS, STARTS_WITH, ENDS_WITH,
        GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, EXISTS, NOT_EXISTS
    }

    private final Map<Operator, BiPredicate<Object, Object>> operators = new EnumMap<>(Operator.class);

    ConditionEvaluator() {
        operators.put(Operator.EQUALS, this::equalsValue);
        operators.put(Operator.NOT_EQUALS, (a, e) -> !equalsValue(a, e));
        operators.put(Operator.IN, (a, e) -> toCollection(e).stream().anyMatch(x -> equalsValue(a, x)));
        operators.put(Operator.NOT_IN, (a, e) -> toCollection(e).stream().noneMatch(x -> equalsValue(a, x)));
        operators.put(Operator.CONTAINS, (a, e) -> asString(a).contains(asString(e)));
        operators.put(Operator.STARTS_WITH, (a, e) -> asString(a).startsWith(asString(e)));
        operators.put(Operator.ENDS_WITH, (a, e) -> asString(a).endsWith(asString(e)));
        operators.put(Operator.GREATER_THAN, (a, e) -> compareNumeric(a, e) > 0);
        operators.put(Operator.GREATER_THAN_OR_EQUAL, (a, e) -> compareNumeric(a, e) >= 0);
        operators.put(Operator.LESS_THAN, (a, e) -> compareNumeric(a, e) < 0);
        operators.put(Operator.LESS_THAN_OR_EQUAL, (a, e) -> compareNumeric(a, e) <= 0);
    }

    boolean evaluate(ConditionConfig condition, EvaluationContext context) {
        Operator operator = Operator.valueOf(condition.operator());

        if (operator == Operator.EXISTS) {
            return context.has(condition.field());
        }
        if (operator == Operator.NOT_EXISTS) {
            return !context.has(condition.field());
        }

        Object actual = context.get(condition.field()).orElse(null);
        if (actual == null) {
            return false;
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
