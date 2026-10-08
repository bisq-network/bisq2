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

package bisq.desktop.main.content.bisq_easy.take_offer.amount;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Transitions;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideRangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The amount step of a buyer who takes a range sell offer. The seller's maximum is derived strictly from the score,
 * without the tolerance of the rule in docs/specifications/trade/bisq-easy-seller-reputation-limit.md. A maker of
 * version 2.1.13 rounds its maximum half up: a score of 79902 gives 400 USD there, and 399 USD in this version.
 */
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
@ResourceLock(value = "Browser", mode = ResourceAccessMode.READ_WRITE)
class TakeOfferAmountControllerTest {
    private static final Market MARKET = MarketRepository.getUSDBitcoinMarket();
    private static final PriceQuote PRICE_QUOTE = PriceQuote.fromFiatPrice(60_000, "USD");
    private static final String SELLERS_PROFILE_ID = "sellers-profile-id";
    // The seller's maximum for this score is 399 USD
    private static final long SELLERS_SCORE = 79_902;

    private ServiceProvider serviceProvider;
    private TakeOfferAmountController controller;
    private Stage stage;

    @BeforeEach
    void setUp() {
        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getPriceQuote()).thenReturn(PRICE_QUOTE);
        ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
        marketPriceByCurrencyMap.put(MARKET, marketPrice);
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
        when(marketPriceService.findMarketPrice(MARKET)).thenReturn(Optional.of(marketPrice));
        when(marketPriceService.findMarketPriceQuote(MARKET)).thenReturn(Optional.of(PRICE_QUOTE));

        ReputationService reputationService = mock(ReputationService.class);
        when(reputationService.getReputationScore(SELLERS_PROFILE_ID)).thenReturn(new ReputationScore(SELLERS_SCORE, 0, 1));

        serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getBondedRolesService().getMarketPriceService()).thenReturn(marketPriceService);
        when(serviceProvider.getUserService().getReputationService()).thenReturn(reputationService);
        when(serviceProvider.getUserService().getUserIdentityService().getSelectedUserIdentity().getUserProfile().getId())
                .thenReturn("buyers-profile-id");

        SettingsService settingsService = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
        // The view asks Browser whether hyperlinks get copied instead of opened
        Browser.initialize(null, settingsService, mock(DontShowAgainService.class));
    }

    @AfterEach
    void tearDown() {
        if (stage != null) {
            WaitForAsyncUtils.asyncFx(() -> {
                // Detaching the view deactivates the controllers, as when the wizard leaves the step
                stage.getScene().setRoot(new Pane());
                stage.close();
            });
            WaitForAsyncUtils.waitForFxEvents();
        }
        Browser.initialize(null, null, null);
        Transitions.setSettingsService(null);
    }

    // The tolerance covers the offer's minimum of 399.60 USD, so the buyer can take the offer, but the seller's maximum
    // of 399 USD is below it. A range which ended at the seller's maximum would allow only 399 USD, which the maker
    // rejects because the amount is outside of its offer.
    @Test
    void buyersAmountIsNotBelowTheMinimumOfARangeOfferWhichTheToleranceCovers() throws Exception {
        showAmountStep(rangeSellOffer(3_996_000, 4_000_000));

        assertThat(controller.getTakersQuoteSideAmount().get()).isEqualTo(Fiat.fromFaceValue(399.6, "USD"));
    }

    // The amount step limits the buyer's amount to the seller's maximum and does not use the tolerance for it
    @Test
    void buyersAmountIsLimitedToTheSellersMaximum() throws Exception {
        showAmountStep(rangeSellOffer(500_000, 4_000_000));

        // The buyer selects the amount which the seller's reputation covers
        WaitForAsyncUtils.asyncFx(() -> controller.onSetReputationBasedAmount()).get(10, TimeUnit.SECONDS);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(controller.getTakersQuoteSideAmount().get()).isEqualTo(Fiat.fromFaceValue(399, "USD"));
    }

    private void showAmountStep(BisqEasyOffer offer) throws Exception {
        WaitForAsyncUtils.asyncFx(() -> {
            controller = new TakeOfferAmountController(serviceProvider, navigationButtonsVisible -> {
            });
            controller.init(offer);
            stage = new Stage();
            stage.setScene(new Scene(controller.getView().getRoot()));
            stage.show();
        }).get(10, TimeUnit.SECONDS);
        // The amount selection applies its values on the next render frames
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(controller.getTakersQuoteSideAmount().get()).isNotNull();
    }

    private static BisqEasyOffer rangeSellOffer(long minAmount, long maxAmount) {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getDirection()).thenReturn(Direction.SELL);
        when(offer.getTakersDirection()).thenReturn(Direction.BUY);
        when(offer.getMakersUserProfileId()).thenReturn(SELLERS_PROFILE_ID);
        when(offer.getAmountSpec()).thenReturn(new QuoteSideRangeAmountSpec(minAmount, maxAmount));
        when(offer.getPriceSpec()).thenReturn(new MarketPriceSpec());
        return offer;
    }
}
