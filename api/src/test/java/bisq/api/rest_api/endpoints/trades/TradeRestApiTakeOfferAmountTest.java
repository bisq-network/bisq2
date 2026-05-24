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

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceProvider;
import bisq.bonded_roles.market_price.MarketPriceProviderInfo;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.support.SupportService;
import bisq.support.mediation.bisq_easy.BisqEasyMediationRequestService;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import jakarta.ws.rs.container.AsyncResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression test for a bug where {@link TradeRestApi#takeOffer} constructed the
 * quote-side {@link Monetary} using {@code market.getBaseCurrencyCode()} ("BTC")
 * instead of {@code market.getQuoteCurrencyCode()} ("USD").
 * <p>
 * This caused the fiat amount to be wrapped in a {@link Coin} (BTC, precision 8)
 * instead of a {@link Fiat} (USD, precision 4). While the raw {@code long} value
 * happened to pass through unchanged to the contract, the semantic type was wrong,
 * making the code fragile to any downstream type check, precision-based rounding,
 * or display formatting.
 */
class TradeRestApiTakeOfferAmountTest {

    private static final Market MARKET = new Market("BTC", "USD", "Bitcoin", "US Dollar");

    @BeforeAll
    static void init_i18n() {
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    @DisplayName("takeOffer must construct quote-side Monetary as Fiat with quote currency code")
    void take_offer_constructs_quote_side_amount_with_quote_currency_code() {
        long baseSideValue = 100_000L;
        long quoteSideValue = 60_000_000L;

        ChatService chatService = mock(ChatService.class);
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        UserService userService = mock(UserService.class);
        SupportService supportService = mock(SupportService.class);
        TradeService tradeService = mock(TradeService.class);

        BisqEasyOfferbookChannelService offerbookChannelService = mock(BisqEasyOfferbookChannelService.class);
        BisqEasyOpenTradeChannelService openTradeChannelService = mock(BisqEasyOpenTradeChannelService.class);
        LeavePrivateChatManager leavePrivateChatManager = mock(LeavePrivateChatManager.class);
        ChatChannelSelectionService selectionService = mock(ChatChannelSelectionService.class);
        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        BannedUserService bannedUserService = mock(BannedUserService.class);
        BisqEasyMediationRequestService mediationService = mock(BisqEasyMediationRequestService.class);
        BisqEasyTradeService bisqEasyTradeService = mock(BisqEasyTradeService.class);

        when(chatService.getBisqEasyOfferbookChannelService()).thenReturn(offerbookChannelService);
        when(chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OFFERBOOK)).thenReturn(selectionService);
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(openTradeChannelService);
        when(chatService.getLeavePrivateChatManager()).thenReturn(leavePrivateChatManager);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getBannedUserService()).thenReturn(bannedUserService);
        when(supportService.getBisqEasyMediationRequestService()).thenReturn(mediationService);
        when(tradeService.getBisqEasyTradeService()).thenReturn(bisqEasyTradeService);

        TradeRestApi api = new TradeRestApi(chatService, marketPriceService, userService, supportService, tradeService);

        NetworkId makerNid = createNetworkId(1);
        BisqEasyOffer offer = new BisqEasyOffer(
                "test-offer-id",
                System.currentTimeMillis(),
                makerNid,
                Direction.SELL,
                MARKET,
                new BaseSideFixedAmountSpec(baseSideValue),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                0,
                "1.0.0",
                "1.0.0"
        );

        BisqEasyOfferbookChannel channel = new BisqEasyOfferbookChannel(MARKET);
        BisqEasyOfferbookMessage message = new BisqEasyOfferbookMessage(
                channel.getId(),
                offer.getMakersUserProfileId(),
                Optional.of(offer),
                Optional.of("offer text"),
                Optional.empty(),
                System.currentTimeMillis(),
                false
        );
        channel.addChatMessage(message);

        ObservableSet<BisqEasyOfferbookChannel> channels = new ObservableSet<>();
        channels.add(channel);
        when(offerbookChannelService.getChannels()).thenReturn(channels);

        NetworkId takerNid = createNetworkId(2);
        UserProfile takerProfile = createUserProfile(takerNid);
        UserIdentity takerIdentity = createUserIdentity(takerProfile, takerNid);
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(takerIdentity);
        when(bannedUserService.isUserProfileBanned(any(UserProfile.class))).thenReturn(false);
        when(bannedUserService.isUserProfileBanned(any(NetworkId.class))).thenReturn(false);

        when(mediationService.selectMediator(any(), any(), any())).thenReturn(Optional.empty());

        PriceQuote priceQuote = new PriceQuote(
                600_000_000L,
                Monetary.from(1_0000_0000L, "BTC"),
                Monetary.from(600_000_000L, "USD")
        );
        MarketPriceProviderInfo providerInfo = new MarketPriceProviderInfo(MarketPriceProvider.COINGECKO, "CoinGecko");
        MarketPrice marketPrice = new MarketPrice(priceQuote, System.currentTimeMillis(), providerInfo);
        when(marketPriceService.findMarketPrice(MARKET)).thenReturn(Optional.of(marketPrice));

        AtomicReference<Monetary> capturedBaseSide = new AtomicReference<>();
        AtomicReference<Monetary> capturedQuoteSide = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedBaseSide.set(invocation.getArgument(2));
            capturedQuoteSide.set(invocation.getArgument(3));
            throw new RuntimeException("intentional-stop: arguments captured");
        }).when(bisqEasyTradeService).takerCreatesProtocol(
                any(), any(), any(), any(), any(), any(), any(), any(), anyLong());

        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        TakeOfferRequest request = new TakeOfferRequest(
                offer.getId(), baseSideValue, quoteSideValue, "MAIN_CHAIN", "SEPA");
        api.takeOffer(request, asyncResponse);

        assertThat(capturedBaseSide.get())
                .as("base-side amount must be a Coin (BTC)")
                .isInstanceOf(Coin.class);
        assertThat(capturedBaseSide.get().getCode()).isEqualTo("BTC");
        assertThat(capturedBaseSide.get().getValue()).isEqualTo(baseSideValue);

        assertThat(capturedQuoteSide.get())
                .as("quote-side amount must be a Fiat (USD), not a Coin — this was the bug")
                .isInstanceOf(Fiat.class);
        assertThat(capturedQuoteSide.get().getCode()).isEqualTo("USD");
        assertThat(capturedQuoteSide.get().getValue()).isEqualTo(quoteSideValue);
    }

    @Test
    @DisplayName("Monetary.from with base vs quote currency code produces different types")
    void monetary_from_base_vs_quote_currency_code_produces_different_types() {
        long value = 60_000_0000L;

        Monetary asBase = Monetary.from(value, MARKET.getBaseCurrencyCode());
        Monetary asQuote = Monetary.from(value, MARKET.getQuoteCurrencyCode());

        assertThat(asBase).isInstanceOf(Coin.class);
        assertThat(asBase.getCode()).isEqualTo("BTC");
        assertThat(asBase.getPrecision()).isEqualTo(8);

        assertThat(asQuote).isInstanceOf(Fiat.class);
        assertThat(asQuote.getCode()).isEqualTo("USD");
        assertThat(asQuote.getPrecision()).isEqualTo(4);

        assertThat(asBase.getValue())
                .as("raw long is identical — the bug was subtle because the value passed through unchanged")
                .isEqualTo(asQuote.getValue());
    }

    private static NetworkId createNetworkId(int port) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port)));
        return new NetworkId(addresses, pubKey);
    }

    private static UserProfile createUserProfile(NetworkId networkId) {
        PubKey pubKey = networkId.getPubKey();
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "test-user", proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static UserIdentity createUserIdentity(UserProfile profile, NetworkId networkId) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        var torKeyPair = TorKeyGeneration.generateKeyPair();
        var i2pKeyPair = I2PKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(profile.getId(), kp, torKeyPair, i2pKeyPair);
        Identity identity = new Identity(profile.getId(), networkId, keyBundle);
        return new UserIdentity(identity, profile);
    }
}
