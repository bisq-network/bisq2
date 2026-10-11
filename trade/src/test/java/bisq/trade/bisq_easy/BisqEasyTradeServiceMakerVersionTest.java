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

package bisq.trade.bisq_easy;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceProviderInfo;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.application.ApplicationVersion;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.collection.ObservableSet;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.TradeRestrictedException;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.BASE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * While a minimum version for trading is required, a node of the maker of an older version acknowledges a take offer
 * request and drops it. The node of the taker refuses such a take (see docs/specifications/trade/trade-restrictions.md).
 * The offers of the test data were created by version 2.1.13.
 */
class BisqEasyTradeServiceMakerVersionTest {
    private static final String REQUIRED_VERSION = ApplicationVersion.getVersion().toString();

    private ObservableSet<AuthorizedAlertData> alerts;
    private UserProfileService userProfileService;
    private BisqEasyTradeService service;
    private BisqEasyOffer offer;

    @BeforeEach
    void setUp() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        alerts = new ObservableSet<>();
        when(serviceProvider.getBondedRolesService().getAlertService().getAuthorizedAlertDataSet()).thenReturn(alerts);
        BisqEasyOpenTradeChannelService channelService = mock(BisqEasyOpenTradeChannelService.class);
        when(serviceProvider.getChatService().getBisqEasyOpenTradeChannelService()).thenReturn(channelService);
        when(channelService.getChannels()).thenReturn(new ObservableSet<>());
        userProfileService = mock(UserProfileService.class);
        when(serviceProvider.getUserService().getUserProfileService()).thenReturn(userProfileService);
        // The taker buys. A market price and a reputation of the seller which covers the amount, for the case that
        // the node of the taker also checks the reputation of the seller.
        MarketPrice usdMarketPrice = new MarketPrice(PriceQuote.fromFiatPrice(100_000, "USD"),
                System.currentTimeMillis(),
                mock(MarketPriceProviderInfo.class));
        when(serviceProvider.getBondedRolesService().getMarketPriceService().findMarketPrice(any()))
                .thenReturn(Optional.of(usdMarketPrice));
        when(serviceProvider.getUserService().getReputationService().getReputationScore(any(String.class)))
                .thenReturn(new ReputationScore(1_000_000, 5, 1));
        service = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP);
        service.initialize();
        offer = offer("offer-id", identity("maker").getNetworkId(), QUOTE_AMOUNT);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void takeOfAMakerOfAnOlderVersionIsRefused() {
        alerts.add(minVersionForTrading(REQUIRED_VERSION));
        makerProfileHasVersion("2.1.13");

        TradeRestrictedException exception = assertThrows(TradeRestrictedException.class, this::take);

        assertEquals(TradeRestrictedException.Restriction.MAKER_VERSION_BELOW_MIN_REQUIRED, exception.getRestriction());
        assertEquals(Optional.of(REQUIRED_VERSION), exception.findMinRequiredVersion());
        // Released mobile clients read this text as a restriction of their own node
        assertFalse(exception.getMessage().contains("For trading you need to have version"));
        assertTrue(service.getTrades().isEmpty());
    }

    // The maker created the offer with an earlier version and published its profile again after its update
    @Test
    void takeOfAMakerWhoseProfileHasTheRequiredVersionIsAccepted() {
        alerts.add(minVersionForTrading(REQUIRED_VERSION));
        makerProfileHasVersion(REQUIRED_VERSION);

        assertDoesNotThrow(this::take);
    }

    @Test
    void takeOfAMakerWithoutKnownProfileIsDecidedByTheVersionOfTheOffer() {
        alerts.add(minVersionForTrading(REQUIRED_VERSION));
        when(userProfileService.findUserProfile(offer.getMakersUserProfileId())).thenReturn(Optional.empty());

        assertThrows(TradeRestrictedException.class, this::take);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "x", "99999999999.1.1"})
    void makerWithAVersionWhichCannotBeReadIsRefused(String version) {
        alerts.add(minVersionForTrading(REQUIRED_VERSION));
        makerProfileHasVersion(version);

        assertThrows(TradeRestrictedException.class, this::take);
    }

    @Test
    void withoutAlertTheVersionOfTheMakerIsNotChecked() {
        makerProfileHasVersion("2.1.13");

        assertDoesNotThrow(this::take);
    }

    private void take() {
        service.takerCreatesProtocol(identity("taker"),
                offer,
                Coin.asBtcFromValue(BASE_AMOUNT),
                Fiat.fromValue(QUOTE_AMOUNT, "USD"),
                offer.getBaseSidePaymentMethodSpecs().get(0),
                offer.getQuoteSidePaymentMethodSpecs().get(0),
                Optional.empty(),
                60_000_0000);
    }

    private void makerProfileHasVersion(String version) {
        UserProfile makerProfile = mock(UserProfile.class);
        when(makerProfile.getApplicationVersion()).thenReturn(version);
        when(userProfileService.findUserProfile(offer.getMakersUserProfileId())).thenReturn(Optional.of(makerProfile));
    }

    private static AuthorizedAlertData minVersionForTrading(String minVersion) {
        return new AuthorizedAlertData("alertId",
                System.currentTimeMillis(),
                AlertType.EMERGENCY,
                Optional.empty(),
                Optional.of("Update to trade"),
                false,
                true,
                Optional.of(minVersion),
                Optional.empty(),
                "1".repeat(40),
                false,
                Optional.empty(),
                AppType.DESKTOP);
    }
}
