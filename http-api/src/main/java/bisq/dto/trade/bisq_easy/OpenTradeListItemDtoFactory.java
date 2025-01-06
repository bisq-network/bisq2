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

package bisq.dto.trade.bisq_easy;


import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.dto.DtoMappings;
import bisq.dto.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelDto;
import bisq.dto.user.profile.UserProfileDto;
import bisq.dto.user.profile.UserProfileItemDto;
import bisq.dto.user.profile.UserProfileItemDtoFactory;
import bisq.dto.user.reputation.ReputationScoreDto;
import bisq.presentation.formatters.DateFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;

import java.util.Optional;

public class OpenTradeListItemDtoFactory {
    public static OpenTradeItemDto create(BisqEasyTrade trade,
                                          BisqEasyOpenTradeChannel channel,
                                          UserProfileService userProfileService,
                                          ReputationService reputationService) {
        if (trade.getContract() == null) {
            throw new IllegalArgumentException("Contract must not be null. Trade: " + trade);
        }

        UserProfile myUserProfile = userProfileService.getManagedUserProfile(channel.getMyUserIdentity().getUserProfile());
        UserProfile peersUserProfile = userProfileService.getManagedUserProfile(channel.getPeer());
        BisqEasyContract contract = trade.getContract();
        long date = contract.getTakeOfferDate();

        String peersUserName = peersUserProfile.getUserName();
        String myUserName = channel.getMyUserIdentity().getUserName();
        String directionalTitle = BisqEasyTradeFormatter.getDirectionalTitle(trade);
        String offerId = channel.getBisqEasyOffer().getId();
        String tradeId = trade.getId();
        String shortTradeId = trade.getShortId();
        String dateString = DateFormatter.formatDate(date);
        String timeString = DateFormatter.formatTime(date);
        String market = trade.getOffer().getMarket().toString();
        long price = BisqEasyTradeUtils.getPriceQuote(trade).getValue();
        String priceString = BisqEasyTradeFormatter.formatPriceWithCode(trade);
        long baseAmount = contract.getBaseSideAmount();
        String baseAmountString = BisqEasyTradeFormatter.formatBaseSideAmount(trade);
        long quoteAmount = contract.getQuoteSideAmount();
        String quoteAmountString = BisqEasyTradeFormatter.formatQuoteSideAmount(trade);

        String bitcoinSettlementMethod = contract.getBaseSidePaymentMethodSpec().getPaymentMethodName();
        String bitcoinSettlementMethodDisplayString = contract.getBaseSidePaymentMethodSpec().getShortDisplayString();
        String fiatPaymentMethod = contract.getQuoteSidePaymentMethodSpec().getPaymentMethodName();
        String fiatPaymentMethodDisplayString = contract.getQuoteSidePaymentMethodSpec().getShortDisplayString();
        boolean isFiatPaymentMethodCustom = contract.getQuoteSidePaymentMethodSpec().getPaymentMethod().isCustomPaymentMethod();

        String myRole = BisqEasyTradeFormatter.getMakerTakerRole(trade);
        ReputationScoreDto reputationScore = DtoMappings.ReputationScoreMapping.fromBisq2Model(reputationService.getReputationScore(peersUserProfile));
        String mediatorUserName = channel.getMediator().map(UserProfile::getUserName).orElse("");

        BisqEasyOpenTradeChannelDto channelDto = DtoMappings.BisqEasyOpenTradeChannelMapping.fromBisq2Model(channel);
        BisqEasyTradeDto tradeDto = DtoMappings.BisqEasyTradeMapping.fromBisq2Model(trade);

        //BisqEasyTradeDto tradeDto = DtoMappings.BisqEasyTradeMapping.fromBisq2Model(trade);

        // BisqEasyContract contract = trade.getContract();
        UserProfileDto myUserProfileDto = DtoMappings.UserProfileMapping.fromBisq2Model(myUserProfile);
        UserProfileDto peersUserProfileDto = DtoMappings.UserProfileMapping.fromBisq2Model(peersUserProfile);

        UserProfileDto makerProfile;
        UserProfileDto takerProfile;
        if (trade.isMaker()) {
            makerProfile = myUserProfileDto;
            takerProfile = peersUserProfileDto;
        } else {
            makerProfile = peersUserProfileDto;
            takerProfile = myUserProfileDto;
        }

        //todo use UserProfile and handle reputation separately
        UserProfileItemDto makerUserProfileItem = UserProfileItemDtoFactory.create(makerProfile, reputationService);
        UserProfileItemDto takerUserProfileItem =  UserProfileItemDtoFactory.create(takerProfile, reputationService);

        Optional<UserProfileItemDto> mediatorUserProfileItem = contract.getMediator()
                .map(userProfile ->  UserProfileItemDtoFactory.create(DtoMappings.UserProfileMapping.fromBisq2Model(userProfile), reputationService));

        return new OpenTradeItemDto(
                channelDto,
                tradeDto,
                makerUserProfileItem,
                takerUserProfileItem,
                mediatorUserProfileItem,
                peersUserName,
                myUserName,
                directionalTitle,
                offerId,
                tradeId,
                shortTradeId,
                dateString,
                timeString,
                market,
                price,
                priceString,
                baseAmount,
                baseAmountString,
                quoteAmount,
                quoteAmountString,
                bitcoinSettlementMethod,
                bitcoinSettlementMethodDisplayString,
                fiatPaymentMethod,
                fiatPaymentMethodDisplayString,
                isFiatPaymentMethodCustom,
                myRole,
                reputationScore,
                mediatorUserName
        );
    }
}