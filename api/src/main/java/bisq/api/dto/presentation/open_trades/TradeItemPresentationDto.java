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

package bisq.api.dto.presentation.open_trades;

import bisq.api.dto.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelDto;
import bisq.api.dto.trade.bisq_easy.BisqEasyTradeDto;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.api.dto.user.reputation.ReputationScoreDto;

import java.util.Optional;

// Presentation DTO
// Similar to bisq.desktop.main.content.bisq_easy.open_trades.OpenTradeListItem
public record TradeItemPresentationDto(
        BisqEasyOpenTradeChannelDto channel,
        BisqEasyTradeDto trade,
        UserProfileDto makerUserProfile,
        UserProfileDto takerUserProfile,
        Optional<UserProfileDto> mediatorUserProfile,
        String directionalTitle,
        String formattedDate,
        String formattedTime,
        String market,
        long price,
        String formattedPrice,
        long baseAmount,
        String formattedBaseAmount,
        long quoteAmount,
        String formattedQuoteAmount,
        String bitcoinSettlementMethod,
        String bitcoinSettlementMethodDisplayString,
        String fiatPaymentMethod,
        String fiatPaymentMethodDisplayString,
        boolean isFiatPaymentMethodCustom,
        String formattedMyRole,
        ReputationScoreDto peersReputationScore) {
}