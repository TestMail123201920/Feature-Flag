package com.company.featureflag.rule.domain;

/** Extensible set of comparison operators used by RuleCondition/RuleEvaluator. */
public enum Operator {
    EQUALS,
    NOT_EQUALS,
    IN,
    NOT_IN,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    EXISTS,
    NOT_EXISTS
}
