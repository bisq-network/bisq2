/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package bisq.api.rest_api.endpoints.trades;

import bisq.api.dto.DtoMappings;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Read-only observations from the node's own open and closed trade stores.
 * The stores are concurrently mutable: this is NOT an atomic snapshot and
 * an empty response cannot justify retrying an ambiguous POST /trades.
 */
final class TradeStatusQuery {
    private TradeStatusQuery() {
    }

    static List<TradeStatusDto> byOffer(Stream<BisqEasyTrade> openTrades,
                                        Stream<BisqEasyClosedTrade> closedTrades,
                                        String offerId) {
        if (offerId == null || !offerId.matches("[A-Za-z0-9_.:-]{1,256}")) {
            throw new IllegalArgumentException("offerId must be a valid identifier");
        }
        Map<String, TradeStatusDto> byId = new HashMap<>();
        openTrades.filter(trade -> offerId.equals(trade.getOffer().getId()))
                .map(trade -> toDto(trade, "OPEN"))
                .forEach(dto -> byId.put(dto.tradeId(), dto));

        // Moving a trade to the closed store may temporarily leave it in
        // both stores. Prefer CLOSED for the same ID during that interval.
        closedTrades.map(BisqEasyClosedTrade::trade)
                .filter(trade -> offerId.equals(trade.getOffer().getId()))
                .map(trade -> toDto(trade, "CLOSED"))
                .forEach(dto -> byId.put(dto.tradeId(), dto));

        return byId.values().stream()
                .sorted(Comparator.comparing(TradeStatusDto::tradeId))
                .toList();
    }

    private static TradeStatusDto toDto(BisqEasyTrade trade, String lifecycle) {
        return new TradeStatusDto(trade.getId(),
                trade.getOffer().getId(),
                DtoMappings.BisqEasyTradeStateMapping.fromBisq2Model(trade.getTradeState()),
                lifecycle);
    }
}
