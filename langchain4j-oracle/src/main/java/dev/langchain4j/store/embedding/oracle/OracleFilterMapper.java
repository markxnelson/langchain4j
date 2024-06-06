package dev.langchain4j.store.embedding.oracle;

import java.util.Collection;
import java.util.stream.Collectors;

import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsGreaterThan;
import dev.langchain4j.store.embedding.filter.comparison.IsGreaterThanOrEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.filter.comparison.IsLessThan;
import dev.langchain4j.store.embedding.filter.comparison.IsLessThanOrEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsNotEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsNotIn;
import dev.langchain4j.store.embedding.filter.logical.And;
import dev.langchain4j.store.embedding.filter.logical.Not;
import dev.langchain4j.store.embedding.filter.logical.Or;

public class OracleFilterMapper {
    public String whereClause(Filter filter) {
        return map(filter);
    }

    private String map(Filter filter) {
        if (filter instanceof IsEqualTo) {
            IsEqualTo eq = (IsEqualTo) filter;
            return String.format("%s == %s", eq.key(), formatValue(eq.comparisonValue()));
        } else if (filter instanceof IsNotEqualTo) {
            IsNotEqualTo ne = (IsNotEqualTo) filter;
            return String.format("%s != %s", ne.key(), formatValue(ne.comparisonValue()));
        } else if (filter instanceof IsGreaterThan) {
            IsGreaterThan gt = (IsGreaterThan) filter;
            return String.format("%s > %s", gt.key(), formatValue(gt.comparisonValue()));
        } else if (filter instanceof IsGreaterThanOrEqualTo) {
            IsGreaterThanOrEqualTo gte = (IsGreaterThanOrEqualTo) filter;
            return String.format("%s >= %s", gte.key(), formatValue(gte.comparisonValue()));
        } else if (filter instanceof IsLessThan) {
            IsLessThan lt = (IsLessThan) filter;
            return String.format("%s < %s", lt.key(), formatValue(lt.comparisonValue()));
        } else if (filter instanceof IsLessThanOrEqualTo) {
            IsLessThanOrEqualTo lte = (IsLessThanOrEqualTo) filter;
            return String.format("%s <= %s", lte.key(), formatValue(lte.comparisonValue()));
        } else if (filter instanceof IsIn) {
            IsIn in = (IsIn) filter;
            return String.format("%s in %s", in.key(), formatValues(in.comparisonValues()));
        } else if (filter instanceof IsNotIn) {
            IsNotIn ni = (IsNotIn) filter;
            return String.format("%s not in %s", ni.key(), formatValues(ni.comparisonValues()));
        } else if (filter instanceof And) {
            And and = (And) filter;
            return String.format("%s && %s", map(and.left()), map(and.right()));
        } else if (filter instanceof Not) {
            Not not = (Not) filter;
            return String.format("not (%s)", map(not.expression()));
        } else if (filter instanceof Or) {
            Or or = (Or) filter;
            return String.format("(%s || %s)", map(or.left()), map(or.right()));
        } else {
            throw new UnsupportedOperationException("Unsupported filter type: " + filter.getClass().getName());
        }
    }

    private String formatValue(Object v) {
        if (v instanceof String) {
            return "'" + v + "'";
        } else {
            return v.toString();
        }
    }

    String formatValues(Collection<?> values) {
        return "(" + values.stream().map(this::formatValue)
                .collect(Collectors.joining(",")) + ")";
    }
}
