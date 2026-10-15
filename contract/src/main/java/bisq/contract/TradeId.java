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

package bisq.contract;

import bisq.security.DigestUtil;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * The ID of a trade is derived from the data of its contract: the ID of the offer, the ID of the network ID of the
 * taker, and the take offer date. Versions up to 2.1.6 created the ID without the take offer date. Version 2.1.7 added
 * the take offer date for trades which it created from 2025-06-01 (UTC) on. A trade keeps the ID with which it was
 * created. See docs/specifications/trade/trade-id.md.
 */
public final class TradeId {
    private TradeId() {
    }

    /**
     * The ID which a trade with this contract gets when it is created.
     */
    public static String fromContract(Contract<?> contract) {
        return create(contract.getOffer().getId(), contract.getTaker().getNetworkId().getId(), contract.getTakeOfferDate());
    }

    /**
     * Whether the trade ID is the ID of the trade with this contract. A trade keeps the ID with which it was created,
     * so the ID without the take offer date of a trade which an earlier version created matches as well.
     */
    public static boolean isIdOfContract(String tradeId, Contract<?> contract) {
        String offerId = contract.getOffer().getId();
        String takerId = contract.getTaker().getNetworkId().getId();
        return tradeId.equals(create(offerId, takerId, contract.getTakeOfferDate())) ||
                tradeId.equals(create(offerId, takerId));
    }

    public static String create(String offerId, String takerPubKeyHash) {
        return create(offerId, takerPubKeyHash, Optional.empty());
    }

    public static String create(String offerId, String takerPubKeyHash, long takeOfferDate) {
        return create(offerId, takerPubKeyHash, Optional.of(takeOfferDate));
    }

    private static String create(String offerId, String takerPubKeyHash, Optional<Long> takeOfferDate) {
        String combined = offerId + takerPubKeyHash + takeOfferDate.map(String::valueOf).orElse("");
        return UUID.nameUUIDFromBytes(DigestUtil.hash(combined.getBytes(StandardCharsets.UTF_8))).toString();
    }
}
