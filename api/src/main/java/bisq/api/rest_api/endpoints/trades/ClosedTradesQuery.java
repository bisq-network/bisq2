/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.api.rest_api.endpoints.trades;

import bisq.api.dto.presentation.closed_trades.ClosedTradeIndexedItem;
import bisq.api.dto.trade.TradeRoleDto;
import bisq.api.dto.trade.bisq_easy.protocol.BisqEasyTradeStateDto;
import bisq.api.rest_api.pagination.SortDirection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ClosedTradesQuery {
    private ClosedTradesQuery() {
    }

    public enum RoleFilter {
        BUYER,
        SELLER;

        public boolean matches(TradeRoleDto tradeRole) {
            return switch (this) {
                case BUYER -> tradeRole == TradeRoleDto.BUYER_AS_MAKER || tradeRole == TradeRoleDto.BUYER_AS_TAKER;
                case SELLER -> tradeRole == TradeRoleDto.SELLER_AS_MAKER || tradeRole == TradeRoleDto.SELLER_AS_TAKER;
            };
        }

        public static Optional<RoleFilter> parse(Optional<String> value) {
            return value
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        try {
                            return RoleFilter.valueOf(s.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            String valid = Arrays.stream(RoleFilter.values())
                                    .map(Enum::name)
                                    .collect(Collectors.joining(", "));
                            throw new IllegalArgumentException(
                                    "Invalid role '" + s + "'. Valid values: " + valid + ".");
                        }
                    });
        }
    }

    public static Set<BisqEasyTradeStateDto> parseStates(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<BisqEasyTradeStateDto> result = EnumSet.noneOf(BisqEasyTradeStateDto.class);
        for (String raw : values) {
            if (raw == null) continue;
            for (String token : raw.split(",")) {
                String s = token.trim();
                if (s.isEmpty()) continue;
                try {
                    result.add(BisqEasyTradeStateDto.valueOf(s.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid state '" + s + "'. Valid values: " +
                                    Arrays.stream(BisqEasyTradeStateDto.values())
                                            .map(Enum::name)
                                            .collect(Collectors.joining(", ")) + ".");
                }
            }
        }
        return result;
    }

    public enum SortField {
        // DATE sorts by the trade-completion timestamp when present (more meaningful for closed
        // trades since users care about when the trade finished), falling back to the take-offer
        // timestamp for trades that never reached a completion state (e.g. cancelled before any
        // completion event was recorded). takeOfferDate is monotonic and always populated, so
        // the fallback guarantees a stable ordering even for never-completed trades.
        // Every field appends the trade id as a final tie-breaker: equal primary values must order
        // deterministically, or items can repeat or vanish across page requests when the source
        // list shifts between fetches.
        DATE(Comparator
                .comparingLong((ClosedTradeIndexedItem item) ->
                        item.dto().tradeCompletedDate().orElse(item.dto().trade().contract().takeOfferDate()))
                .thenComparing(item -> item.dto().trade().id())),
        MARKET(Comparator.comparing(ClosedTradeIndexedItem::market, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(item -> item.dto().trade().id())),
        QUOTE_AMOUNT(Comparator.comparingLong((ClosedTradeIndexedItem item) -> item.dto().quoteAmount())
                .thenComparing(item -> item.dto().trade().id())),
        BASE_AMOUNT(Comparator.comparingLong((ClosedTradeIndexedItem item) -> item.dto().baseAmount())
                .thenComparing(item -> item.dto().trade().id())),
        ROLE(Comparator.comparing((ClosedTradeIndexedItem item) -> item.dto().trade().tradeRole())
                .thenComparing(item -> item.dto().trade().id()));

        private final Comparator<ClosedTradeIndexedItem> comparator;

        SortField(Comparator<ClosedTradeIndexedItem> comparator) {
            this.comparator = comparator;
        }

        public Comparator<ClosedTradeIndexedItem> comparator(SortDirection direction) {
            return direction == SortDirection.ASC ? comparator : comparator.reversed();
        }

        public static SortField parse(Optional<String> value, SortField fallback) {
            return value
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        try {
                            return SortField.valueOf(s.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            String valid = Arrays.stream(SortField.values())
                                    .map(Enum::name)
                                    .collect(Collectors.joining(", "));
                            throw new IllegalArgumentException(
                                    "Invalid sort field '" + s + "'. Valid values: " + valid + ".");
                        }
                    })
                    .orElse(fallback);
        }
    }

    public static List<ClosedTradeIndexedItem> apply(List<ClosedTradeIndexedItem> items,
                                                     Optional<String> search,
                                                     Optional<RoleFilter> role,
                                                     Set<BisqEasyTradeStateDto> states,
                                                     SortField sortBy,
                                                     SortDirection direction) {
        Stream<ClosedTradeIndexedItem> stream = items.stream();
        if (role.isPresent()) {
            RoleFilter r = role.get();
            stream = stream.filter(item -> r.matches(item.dto().trade().tradeRole()));
        }
        if (!states.isEmpty()) {
            stream = stream.filter(item -> states.contains(item.dto().trade().tradeState()));
        }
        Optional<String> needle = search
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT));
        if (needle.isPresent()) {
            stream = stream.filter(item -> matches(item, needle.get()));
        }
        return stream.sorted(sortBy.comparator(direction)).toList();
    }

    private static boolean matches(ClosedTradeIndexedItem item, String needleLower) {
        // The blob is precomputed lowercase over all searchable fields (see ClosedTradeIndexedItem.of),
        // so filtering is one contains() per item instead of lowercasing each field per keystroke.
        return item.searchBlob().contains(needleLower);
    }
}
