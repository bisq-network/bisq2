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

package bisq.http_api.web_socket.domain.offerbook;

import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.http_api.rest_api.domain.offerbook.OfferListItemDto;
import bisq.http_api.rest_api.domain.offerbook.ReputationScoreDto;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.util.StringUtils;
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.http_api.web_socket.subscription.Topic.OFFERS;

@Slf4j
public class OffersWebSocketService extends BaseWebSocketService {
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final Set<Pin> pins = new HashSet<>();

    public OffersWebSocketService(ObjectMapper objectMapper,
                                     SubscriberRepository subscriberRepository,
                                     ChatService chatService,
                                     UserService userService,
                                     BondedRolesService bondedRolesService) {
        super(objectMapper, subscriberRepository, OFFERS);

        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        userProfileService = userService.getUserProfileService();
        userIdentityService = userService.getUserIdentityService();
        reputationService = userService.getReputationService();
        marketPriceService = bondedRolesService.getMarketPriceService();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        // The channels is a static list and does not change at runtime
        bisqEasyOfferbookChannelService.getChannels().forEach(channel -> {
            String quoteCurrencyCode = channel.getMarket().getQuoteCurrencyCode();
            pins.add(channel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void add(BisqEasyOfferbookMessage message) {
                    send(quoteCurrencyCode, message, ModificationType.ADDED);
                }

                @Override
                public void remove(Object element) {
                    if (element instanceof BisqEasyOfferbookMessage message) {
                        send(quoteCurrencyCode, message, ModificationType.REMOVED);
                    }
                }

                @Override
                public void clear() {
                    throw new NotImplementedException("Clear channel.getChatMessages() is not expected to be used");
                }
            }));
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        pins.forEach(Pin::unbind);
        pins.clear();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(bisqEasyOfferbookChannelService.getChannels().stream());
    }

    public Optional<String> getJsonPayload(Stream<BisqEasyOfferbookChannel> channels) {
        ArrayList<OfferListItemDto> payload = channels
                .flatMap(channel ->
                        channel.getChatMessages().stream()
                                .map(this::toOfferListItemDto))
                .collect(Collectors.toCollection(ArrayList::new));
        return toJson(payload);
    }

    private void send(String quoteCurrencyCode,
                      BisqEasyOfferbookMessage message,
                      ModificationType modificationType) {
        OfferListItemDto item = toOfferListItemDto(message);
        // The payload is defined as a list to support batch data delivery at subscribe.
        ArrayList<OfferListItemDto> payload = new ArrayList<>(List.of(item));
        toJson(payload).ifPresent(json -> {
            subscriberRepository.findSubscribers(topic, quoteCurrencyCode)
                    .ifPresent(subscribers -> subscribers
                            .forEach(subscriber -> send(json, subscriber, modificationType)));
        });
    }

    private OfferListItemDto toOfferListItemDto(BisqEasyOfferbookMessage message) {
        BisqEasyOffer bisqEasyOffer = message.getBisqEasyOffer().orElseThrow();
        Market market = bisqEasyOffer.getMarket();

        long date = message.getDate();
        String formattedDate = DateFormatter.formatDateTime(new Date(date), DateFormat.MEDIUM, DateFormat.SHORT,
                true, " " + Res.get("temporal.at") + " ");
        String authorUserProfileId = message.getAuthorUserProfileId();
        Optional<UserProfile> senderUserProfile = userProfileService.findUserProfile(authorUserProfileId);
        String nym = senderUserProfile.map(UserProfile::getNym).orElse("");
        String userName = senderUserProfile.map(UserProfile::getUserName).orElse("");

        ReputationScoreDto reputationScore = senderUserProfile.flatMap(reputationService::findReputationScore)
                .map(score -> new ReputationScoreDto(
                        score.getTotalScore(),
                        score.getFiveSystemScore(),
                        score.getRanking()
                ))
                .orElse(new ReputationScoreDto(0, 0, 0));
        AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
        PriceSpec priceSpec = bisqEasyOffer.getPriceSpec();
        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        String formattedQuoteAmount = OfferAmountFormatter.formatQuoteAmount(
                marketPriceService,
                amountSpec,
                priceSpec,
                market,
                hasAmountRange,
                true
        );
        String formattedPrice = PriceSpecFormatter.getFormattedPriceSpec(priceSpec, true);

        List<String> quoteSidePaymentMethods = PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.getQuoteSidePaymentMethodSpecs())
                .stream()
                .map(PaymentMethod::getName)
                .collect(Collectors.toList());

        List<String> baseSidePaymentMethods = PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.getBaseSidePaymentMethodSpecs())
                .stream()
                .map(PaymentMethod::getName)
                .collect(Collectors.toList());

        String supportedLanguageCodes = Joiner.on(",").join(bisqEasyOffer.getSupportedLanguageCodes());
        boolean isMyMessage = message.isMyMessage(userIdentityService);
        Direction direction = bisqEasyOffer.getDirection();
        String offerTitle = getOfferTitle(message, direction, isMyMessage);
        String messageId = message.getId();
        String offerId = bisqEasyOffer.getId();
        return new OfferListItemDto(messageId,
                offerId,
                isMyMessage,
                direction,
                market.getQuoteCurrencyCode(),
                offerTitle,
                date,
                formattedDate,
                nym,
                userName,
                reputationScore,
                formattedQuoteAmount,
                formattedPrice,
                quoteSidePaymentMethods,
                baseSidePaymentMethods,
                supportedLanguageCodes);
    }

    private String getOfferTitle(BisqEasyOfferbookMessage message, Direction direction, boolean isMyMessage) {
        if (isMyMessage) {
            String directionString = StringUtils.capitalize(Res.get("offer." + direction.name().toLowerCase()));
            return Res.get("bisqEasy.tradeWizard.review.chatMessage.myMessageTitle", directionString);
        } else {
            return message.getText();
        }
    }
}
