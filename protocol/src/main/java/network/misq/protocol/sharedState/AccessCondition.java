package network.misq.protocol.sharedState;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Streams;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccessCondition implements Predicate<Set<String>> {
    private final String expr;
    private final Predicate<Set<String>> cond;
    private final boolean isConjunction;

    private AccessCondition(String expr, Predicate<Set<String>> cond, boolean isConjunction) {
        this.expr = expr;
        this.cond = cond;
        this.isConjunction = isConjunction;
    }

    public AccessCondition and(AccessCondition other) {
        return new AccessCondition(conjunctionExpr() + " & " + other.conjunctionExpr(), cond.and(other.cond), true);
    }

    public AccessCondition or(AccessCondition other) {
        return new AccessCondition(expr + " | " + other.expr, cond.or(other.cond), false);
    }

    public DPF toDPF() {
        return (DPF) (this instanceof DPF ? this : parse(expr, null, true));
    }

    public static DPF all() {
        return DPF.ALL;
    }

    public static DPF none() {
        return DPF.NONE;
    }

    public static DPF atom(String name) {
        return DPF.conjunction(ImmutableSortedSet.of(name));
    }

    public static AccessCondition parse(String expr, Set<String> allowedNames) {
        return parse(expr, Preconditions.checkNotNull(allowedNames), false);
    }

    private static AccessCondition parse(String expr, @Nullable Set<String> allowedNames, boolean toDPF) {
        return bracketRespectingSplit(expr, '|')
                .map(conj -> parseConjunction(conj, allowedNames, toDPF))
                .reduce((p, q) -> toDPF ? ((DPF) p).or((DPF) q) : p.or(q)).orElseThrow();
    }

    private static AccessCondition parseConjunction(String expr, @Nullable Set<String> allowedNames, boolean toDPF) {
        return bracketRespectingSplit(expr, '&')
                .map(term -> parseTrimmedTerm(term, allowedNames, toDPF))
                .reduce((p, q) -> toDPF ? ((DPF) p).and((DPF) q) : p.and(q)).orElseThrow();
    }

    private static AccessCondition parseTrimmedTerm(String expr, @Nullable Set<String> allowedNames, boolean toDPF) {
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return parse(expr.substring(1, expr.length() - 1), allowedNames, toDPF);
        }
        if (expr.equals("ALL")) {
            return DPF.ALL;
        }
        if (expr.equals("NONE")) {
            return DPF.NONE;
        }
        Preconditions.checkArgument(!expr.isEmpty(), "Expected: non-empty term");
        Preconditions.checkArgument(allowedNames == null || allowedNames.contains(expr),
                "Unrecognized/invalid identifier: %s", expr);
        return atom(expr);
    }

    private static Stream<String> bracketRespectingSplit(String expr, char sep) {
        //noinspection UnstableApiUsage
        return Streams.stream(new AbstractIterator<>() {
            private int index = -1, nesting = 0;

            @Override
            protected String computeNext() {
                int start = index + 1;
                char ch;
                while (++index < expr.length() && ((ch = expr.charAt(index)) != sep || nesting > 0)) {
                    nesting += ch == '(' ? 1 : ch == ')' ? -1 : 0;
                }
                return start <= expr.length() ? expr.substring(start, index).trim() : endOfData();
            }
        });
    }

    @Override
    public boolean test(Set<String> names) {
        return cond.test(names);
    }

    @Override
    public String toString() {
        return expr;
    }

    private String conjunctionExpr() {
        return isConjunction ? expr : "(" + expr + ")";
    }

    /**
     * An {@link AccessCondition} in <i>disjunctive prime form</i>, that is, a disjunction of mutually incomparable
     * conjunctions of atoms. For example, the condition <pre> (A & B) | (B & C)</pre> is in DPF but the condition
     * <pre> (A & B) | B</pre> is not, since {@code A & B} entails {@code B}.
     * <p>
     * Any access condition may be converted to an equivalent one in DPF via the method
     * {@link AccessCondition#toDPF()} and any two equivalent access conditions will have equal DPFs. That is to say,
     * <pre>
     *     cond1.test(s) == cond2.test(s), <i>for every </i>s<i> in </i>Set&lt;String&gt;<i> of atoms deemed true</i>
     * </pre>
     * if and only if
     * <pre>
     *     cond1.toDPF().equals(cond2.toDPF()).
     * </pre>
     */
    public static final class DPF extends AccessCondition {
        @SuppressWarnings("UnstableApiUsage")
        private static final Comparator<SortedSet<String>> LEXICOGRAPHICAL_ORDERING = Comparators.lexicographical(
                Comparator.<String>naturalOrder())::compare;
        private static final DPF ALL = conjunction(ImmutableSortedSet.of());
        private static final DPF NONE = new DPF(ImmutableSortedSet.of());

        private final SortedSet<SortedSet<String>> antichain;

        private DPF(SortedSet<SortedSet<String>> antichain) {
            super(toExpr(antichain), set -> antichain.stream().anyMatch(set::containsAll), antichain.size() <= 1);
            this.antichain = antichain;
        }

        public DPF and(DPF other) {
            return new DPF(and(antichain, other.antichain));
        }

        public DPF or(DPF other) {
            return new DPF(or(antichain, other.antichain));
        }

        private static String toExpr(SortedSet<SortedSet<String>> antichain) {
            return antichain.isEmpty() ? "NONE" : antichain.first().isEmpty() ? "ALL" : antichain.stream()
                    .map(set -> String.join(" & ", set))
                    .collect(Collectors.joining(" | "));
        }

        private static DPF conjunction(SortedSet<String> set) {
            return new DPF(ImmutableSortedSet.orderedBy(LEXICOGRAPHICAL_ORDERING).add(set).build());
        }

        private static SortedSet<SortedSet<String>> or(SortedSet<SortedSet<String>> antichain, SortedSet<String> newSet) {
            if (antichain.stream().anyMatch(newSet::containsAll)) {
                return antichain;
            }
            var builder = ImmutableSortedSet.orderedBy(LEXICOGRAPHICAL_ORDERING);
            builder.addAll(antichain.stream().filter(set -> !set.containsAll(newSet)).iterator());
            builder.add(newSet);
            return builder.build();
        }

        private static SortedSet<SortedSet<String>> or(SortedSet<SortedSet<String>> antichain, Iterable<SortedSet<String>> newSets) {
            for (var newSet : newSets) {
                antichain = or(antichain, newSet);
            }
            return antichain;
        }

        private static SortedSet<SortedSet<String>> and(Iterable<SortedSet<String>> sets1, Iterable<SortedSet<String>> sets2) {
            SortedSet<SortedSet<String>> antichain = ImmutableSortedSet.of();
            for (var set1 : sets1) {
                for (var set2 : sets2) {
                    var setUnion = ImmutableSortedSet.<String>naturalOrder().addAll(set1).addAll(set2).build();
                    antichain = or(antichain, setUnion);
                }
            }
            return antichain;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof DPF && toString().equals(obj.toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
}
