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

package bisq.desktop.main.content.bisq_easy;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.currency.Market;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.i18n.Res;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.payment_method.PaymentMethodSpecFormatter;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;

import java.util.List;
import java.util.Optional;

public class BisqEasyServiceUtil {
    public static boolean isMaker(ServiceProvider serviceProvider, BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(serviceProvider.getUserService().getUserIdentityService().getMyUserProfileIds());
    }

    public static Optional<BisqEasyTrade> findTradeFromChannel(ServiceProvider serviceProvider, BisqEasyOpenTradeChannel channel) {
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        BisqEasyOffer bisqEasyOffer = channel.getBisqEasyOffer();
        boolean maker = isMaker(serviceProvider, bisqEasyOffer);
        UserProfile peerUserProfile = channel.getPeer();
        NetworkId takerNetworkId = maker ? peerUserProfile.getNetworkId() : myUserIdentity.getUserProfile().getNetworkId();
        String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
        return serviceProvider.getTradeService().getBisqEasyTradeService().findTrade(tradeId);
    }

    public static boolean offerMatchesMinRequiredReputationScore(ReputationService reputationService,
                                                                 BisqEasyService bisqEasyService,
                                                                 UserIdentityService userIdentityService,
                                                                 UserProfileService userProfileService,
                                                                 BisqEasyOffer peersOffer) {
        if (peersOffer.getDirection().isSell()) {
            Optional<UserProfile> optionalMakersUserProfile = userProfileService.findUserProfile(peersOffer.getMakersUserProfileId());
            if (optionalMakersUserProfile.isEmpty()) {
                return false;
            }
            long makerAsSellersScore = reputationService.getReputationScore(optionalMakersUserProfile.get()).getTotalScore();
            long myMinRequiredScore = bisqEasyService.getMinRequiredReputationScore().get();
            // Maker as seller's score must be > than my required score (as buyer)
            return makerAsSellersScore >= myMinRequiredScore;
        } else {
            // My score (as offer is a buy offer, I am the seller) must be > as offers required score
            long myScoreAsSeller = reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile()).getTotalScore();
            long offersRequiredScore = OfferOptionUtil.findRequiredTotalReputationScore(peersOffer).orElse(0L);
            return myScoreAsSeller >= offersRequiredScore;
        }
    }

    public static String createOfferBookMessageText(boolean isMyMessage,
                                                    String messageOwnerNickName,
                                                    MarketPriceService marketPriceService,
                                                    Direction direction,
                                                    Market market,
                                                    List<FiatPaymentMethod> fiatPaymentMethods,
                                                    AmountSpec amountSpec,
                                                    PriceSpec priceSpec) {
        String paymentMethodNames = PaymentMethodSpecFormatter.fromPaymentMethods(fiatPaymentMethods);
        return createOfferBookMessageText(isMyMessage,
                messageOwnerNickName,
                marketPriceService,
                direction,
                market,
                paymentMethodNames,
                amountSpec,
                priceSpec);
    }

    public static String createOfferBookMessageText(boolean isMyMessage,
                                                    String messageOwnerNickName,
                                                    MarketPriceService marketPriceService,
                                                    Direction direction,
                                                    Market market,
                                                    String paymentMethodNames,
                                                    AmountSpec amountSpec,
                                                    PriceSpec priceSpec) {
        String priceInfo = getFormattedPriceSpec(priceSpec);
        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        String quoteAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, amountSpec, priceSpec, market, hasAmountRange, true);
        return buildOfferBookMessage(isMyMessage, messageOwnerNickName, direction, quoteAmountAsString, paymentMethodNames, priceInfo);
    }

    public static String getFormattedPriceSpec(PriceSpec priceSpec) {
        String priceInfo;
        if (priceSpec instanceof FixPriceSpec) {
            FixPriceSpec fixPriceSpec = (FixPriceSpec) priceSpec;
            String price = PriceFormatter.formatWithCode(fixPriceSpec.getPriceQuote());
            priceInfo = Res.get("bisqEasy.tradeWizard.review.chatMessage.fixPrice", price);
        } else if (priceSpec instanceof FloatPriceSpec) {
            FloatPriceSpec floatPriceSpec = (FloatPriceSpec) priceSpec;
            String percent = PercentageFormatter.formatToPercentWithSymbol(floatPriceSpec.getPercentage());
            priceInfo = Res.get("bisqEasy.tradeWizard.review.chatMessage.floatPrice", percent);
        } else {
            priceInfo = Res.get("bisqEasy.tradeWizard.review.chatMessage.marketPrice");
        }
        return priceInfo;
    }

    private static String buildOfferBookMessage(boolean isMyMessage,
                                                String messageOwnerNickName,
                                                Direction direction,
                                                String quoteAmount,
                                                String paymentMethods,
                                                String price) {
        if (isMyMessage) {
            String directionString = StringUtils.capitalize(Res.get("offer." + direction.name().toLowerCase()));
            return Res.get("bisqEasy.tradeWizard.review.chatMessage.myMessage", directionString, quoteAmount, paymentMethods, price);
        }

        return direction == Direction.BUY
                ? Res.get("bisqEasy.tradeWizard.review.chatMessage.peerMessage.sell", messageOwnerNickName, quoteAmount, paymentMethods, price)
                : Res.get("bisqEasy.tradeWizard.review.chatMessage.peerMessage.buy", messageOwnerNickName, quoteAmount, paymentMethods, price);
    }
}
