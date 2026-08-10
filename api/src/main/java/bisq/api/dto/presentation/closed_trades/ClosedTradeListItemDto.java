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

package bisq.api.dto.presentation.closed_trades;

import bisq.api.dto.common.monetary.PriceQuoteDto;
import bisq.api.dto.trade.TradeRoleDto;
import bisq.api.dto.trade.bisq_easy.protocol.BisqEasyTradeStateDto;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.api.dto.user.reputation.ReputationScoreDto;

import java.util.Optional;

// Slim wire DTO for the closed trades paginated endpoint.
// Drops fields derivable on the client: market (use priceQuote.market), directionalTitle,
// formattedDate/formattedTime (derive from contract.takeOfferDate), formattedMyRole and
// isBuyer (derive from trade.tradeRole), isOnChainSettlement (compare bitcoinSettlementMethod
// to "MAIN_CHAIN"), formattedPriceSpec (clients can format from the offer's price spec
// fetched separately if a price-spec suffix is needed in the UI), and payment-method display
// strings (client owns i18n; resolves from the paymentMethodName). Maker/taker user profiles
// and the peer reputation score are kept as full DTOs so the client can render the catHash
// avatar and StarRating without a follow-up lookup.
public record ClosedTradeListItemDto(
        TradeSlimDto trade,
        UserProfileDto makerUserProfile,
        UserProfileDto takerUserProfile,
        Optional<UserProfileDto> mediatorUserProfile,
        PriceQuoteDto priceQuote,
        long baseAmount,
        long quoteAmount,
        String bitcoinSettlementMethod,
        String fiatPaymentMethod,
        ReputationScoreDto peersReputationScore,
        Optional<String> paymentAccountData,
        Optional<String> bitcoinPaymentData,
        Optional<String> paymentProof,
        Optional<Long> tradeCompletedDate
) {
    public record TradeSlimDto(
            String id,
            TradeRoleDto tradeRole,
            BisqEasyTradeStateDto tradeState,
            ContractSlimDto contract
    ) {
    }

    public record ContractSlimDto(long takeOfferDate) {
    }
}
