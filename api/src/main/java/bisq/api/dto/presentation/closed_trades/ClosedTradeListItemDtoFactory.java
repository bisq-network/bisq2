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

import bisq.api.dto.DtoMappings;
import bisq.api.dto.common.monetary.PriceQuoteDto;
import bisq.api.dto.trade.TradeRoleDto;
import bisq.api.dto.trade.bisq_easy.protocol.BisqEasyTradeStateDto;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.api.dto.user.reputation.ReputationScoreDto;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;

import java.util.Optional;

public final class ClosedTradeListItemDtoFactory {
    private ClosedTradeListItemDtoFactory() {
    }

    public static ClosedTradeIndexedItem create(BisqEasyClosedTrade closedTrade,
                                                ReputationService reputationService) {
        BisqEasyTrade trade = closedTrade.trade();
        BisqEasyContract contract = trade.getContract();
        UserProfile myUserProfile = closedTrade.myUserProfile();
        UserProfile peersUserProfile = closedTrade.peerUserProfile();

        TradeRoleDto tradeRoleDto = DtoMappings.TradeRoleMapping.fromBisq2Model(trade.getTradeRole());
        BisqEasyTradeStateDto tradeStateDto = DtoMappings.BisqEasyTradeStateMapping.fromBisq2Model(trade.getTradeState());

        UserProfileDto myUserProfileDto = DtoMappings.UserProfileMapping.fromBisq2Model(myUserProfile);
        UserProfileDto peersUserProfileDto = DtoMappings.UserProfileMapping.fromBisq2Model(peersUserProfile);
        UserProfileDto makerUserProfile = trade.isMaker() ? myUserProfileDto : peersUserProfileDto;
        UserProfileDto takerUserProfile = trade.isMaker() ? peersUserProfileDto : myUserProfileDto;
        Optional<UserProfileDto> mediatorUserProfile = contract.getMediator()
                .map(DtoMappings.UserProfileMapping::fromBisq2Model);

        PriceQuoteDto priceQuoteDto = DtoMappings.PriceQuoteMapping.fromBisq2Model(trade.getPriceQuote());

        String bitcoinSettlementMethod = contract.getBaseSidePaymentMethodSpec().getPaymentMethodName();
        String fiatPaymentMethod = contract.getQuoteSidePaymentMethodSpec().getPaymentMethodName();

        ReputationScore peersReputationScore = reputationService.getReputationScore(peersUserProfile.getId());
        ReputationScoreDto peersReputationScoreDto = DtoMappings.ReputationScoreMapping.fromBisq2Model(peersReputationScore);

        ClosedTradeListItemDto dto = new ClosedTradeListItemDto(
                new ClosedTradeListItemDto.TradeSlimDto(
                        trade.getId(),
                        tradeRoleDto,
                        tradeStateDto,
                        new ClosedTradeListItemDto.ContractSlimDto(contract.getTakeOfferDate())
                ),
                makerUserProfile,
                takerUserProfile,
                mediatorUserProfile,
                priceQuoteDto,
                contract.getBaseSideAmount(),
                contract.getQuoteSideAmount(),
                bitcoinSettlementMethod,
                fiatPaymentMethod,
                peersReputationScoreDto,
                Optional.ofNullable(trade.getPaymentAccountData().get()),
                Optional.ofNullable(trade.getBitcoinPaymentData().get()),
                Optional.ofNullable(trade.getPaymentProof().get()),
                trade.getTradeCompletedDate()
        );

        String market = trade.getOffer().getMarket().toString();
        String directionalTitle = BisqEasyTradeFormatter.getDirectionalTitle(trade);
        String formattedMyRole = BisqEasyTradeFormatter.getMakerTakerRole(trade);
        String formattedPrice = BisqEasyTradeFormatter.formatPriceWithCode(trade);
        String formattedBaseAmount = BisqEasyTradeFormatter.formatBaseSideAmount(trade);
        String formattedQuoteAmount = BisqEasyTradeFormatter.formatQuoteSideAmount(trade);
        String bitcoinSettlementMethodDisplayString = contract.getBaseSidePaymentMethodSpec().getShortDisplayString();
        String fiatPaymentMethodDisplayString = contract.getQuoteSidePaymentMethodSpec().getShortDisplayString();

        return new ClosedTradeIndexedItem(
                dto,
                market,
                directionalTitle,
                formattedMyRole,
                formattedPrice,
                formattedBaseAmount,
                formattedQuoteAmount,
                bitcoinSettlementMethodDisplayString,
                fiatPaymentMethodDisplayString);
    }
}
