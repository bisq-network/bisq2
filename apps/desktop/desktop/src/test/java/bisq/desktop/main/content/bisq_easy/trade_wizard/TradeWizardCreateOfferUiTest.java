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

package bisq.desktop.main.content.bisq_easy.trade_wizard;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.testutil.DesktopUiTest;
import bisq.i18n.Res;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.settings.CookieKey;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives the Bisq Easy create offer wizard with animations enabled, as a user does in the desktop app.
 */
@DesktopUiTest
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
@ResourceLock(value = "Browser", mode = ResourceAccessMode.READ_WRITE)
class TradeWizardCreateOfferUiTest {
    private static final Market MARKET = MarketRepository.getUSDBitcoinMarket();
    private static final String PROFILE_ID = "0123456789abcdef0123456789abcdef01234567";
    private static final long STEP_TIMEOUT_SECONDS = 10;
    // Longer than the hide animations of WizardOverlay and Transitions.removeEffect (300 ms)
    private static final long ANIMATIONS_DONE_MS = 1000;

    private BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private Stage applicationStage;

    @Start
    void start(Stage primaryStage) {
        SettingsService settingsService = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(true));
        when(settingsService.getSupportedLanguageTags()).thenReturn(new ObservableSet<>(List.of("en")));
        // The user has created an offer in this market before, so the payment methods are preselected
        when(settingsService.getCookie().asString(eq(CookieKey.CREATE_OFFER_METHODS), anyString()))
                .thenReturn(Optional.of("ZELLE"));
        Transitions.setSettingsService(settingsService);
        Navigation.init(settingsService);
        Browser.initialize(null, settingsService, mock(DontShowAgainService.class));

        PriceQuote priceQuote = PriceQuote.fromFiatPrice(60_000, "USD");
        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getPriceQuote()).thenReturn(priceQuote);
        when(marketPrice.getMarket()).thenReturn(MARKET);
        ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
        marketPriceByCurrencyMap.put(MARKET, marketPrice);
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
        when(marketPriceService.findMarketPrice(MARKET)).thenReturn(Optional.of(marketPrice));
        when(marketPriceService.findMarketPriceQuote(MARKET)).thenReturn(Optional.of(priceQuote));

        ReputationService reputationService = mock(ReputationService.class);
        when(reputationService.getReputationScore(anyString())).thenReturn(ReputationScore.NONE);
        when(reputationService.getReputationScore(any(UserProfile.class))).thenReturn(ReputationScore.NONE);
        when(reputationService.getScoreByUserProfileId()).thenReturn(new ObservableHashMap<>());

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getSettingsService()).thenReturn(settingsService);
        when(serviceProvider.getBondedRolesService().getMarketPriceService()).thenReturn(marketPriceService);
        when(serviceProvider.getUserService().getReputationService()).thenReturn(reputationService);
        when(serviceProvider.getUserService().getUserIdentityService().getSelectedUserIdentity().getNickName())
                .thenReturn("maker");
        when(serviceProvider.getUserService().getUserIdentityService().getSelectedUserIdentity().getUserProfile().getId())
                .thenReturn(PROFILE_ID);
        when(serviceProvider.getUserService().getUserIdentityService().getSelectedUserIdentity().getUserProfile()
                .getNetworkId().getPubKey().getId())
                .thenReturn(PROFILE_ID);

        BisqEasyOfferbookChannel channel = new BisqEasyOfferbookChannel(MARKET);
        Observable<ChatChannel<? extends ChatMessage>> selectedChannel = new Observable<>(channel);
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelSelectionService().getSelectedChannel())
                .thenReturn(selectedChannel);
        bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
        when(bisqEasyOfferbookChannelService.getChannels()).thenReturn(new ObservableSet<>(List.of(channel)));
        when(bisqEasyOfferbookChannelService.findChannel(MARKET)).thenReturn(Optional.of(channel));
        // As in the app, the result is available at once: the data service adds the message to the local store and
        // does not wait for the broadcast.
        when(bisqEasyOfferbookChannelService.publishChatMessage(any(), any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(mock(BroadcastResult.class)));

        // OverlayView keeps listeners on the window of the application. A window of its own keeps them away from the
        // primary stage, which the other UI tests use.
        Pane applicationRoot = new Pane();
        applicationStage = new Stage();
        applicationStage.setScene(new Scene(applicationRoot, 1200, 760));
        applicationStage.show();
        new OverlayController(serviceProvider, applicationRoot);
    }

    @AfterEach
    void tearDown(FxRobot robot) {
        robot.interact(() -> OverlayController.hide());
        // Let the hide animations finish while the animation settings are still set
        sleep(ANIMATIONS_DONE_MS);
        robot.interact(() -> {
            OverlayController.getInstance().onDeactivateInternal();
            applicationStage.close();
        });
        Browser.initialize(null, null, null);
        Transitions.setSettingsService(null);
    }

    // The user creates a second offer in the same session and double clicks Next at the payment methods step. The
    // second click hits the same button, which is the "Create offer" button of the review step by then.
    @Test
    void successOverlayStaysVisibleWhenSecondOfferIsCreatedWithFastClicks(FxRobot robot) throws Exception {
        openWizard(robot);
        for (int i = 0; i < 3; i++) {
            robot.clickOn(button(robot, Res.get("action.next")));
        }
        robot.clickOn(button(robot, Res.get("bisqEasy.tradeWizard.review.nextButton.createOffer")));
        waitUntilShowing(robot, createOfferSuccessButtonText());
        // The overlay slides in from the top
        sleep(ANIMATIONS_DONE_MS);
        assertCreateOfferSuccessOverlayIsShown(robot);

        robot.clickOn(button(robot, createOfferSuccessButtonText()));
        sleep(ANIMATIONS_DONE_MS);

        openWizard(robot);
        // Amount and price, then payment methods
        Button nextButton = button(robot, Res.get("action.next"));
        robot.clickOn(nextButton);
        robot.clickOn(nextButton);
        robot.interact(() -> {
            nextButton.fire();
            nextButton.fire();
        });
        sleep(ANIMATIONS_DONE_MS);
        WaitForAsyncUtils.waitForFxEvents();

        verify(bisqEasyOfferbookChannelService, times(2)).publishChatMessage(any(), any());
        assertCreateOfferSuccessOverlayIsShown(robot);
    }

    private void openWizard(FxRobot robot) throws TimeoutException {
        robot.interact(() -> Navigation.navigateTo(NavigationTarget.BISQ_EASY_TRADE_WIZARD,
                new TradeWizardController.InitData(true)));
        // The robot can only click when the display animation of the overlay window has finished
        WaitForAsyncUtils.waitFor(STEP_TIMEOUT_SECONDS, TimeUnit.SECONDS, () -> OverlayController.getInstance().isShown());
        waitUntilShowing(robot, Res.get("action.next"));
    }

    private void assertCreateOfferSuccessOverlayIsShown(FxRobot robot) {
        Node overlay = createOfferSuccessOverlay(robot);
        assertThat(overlay.isVisible()).as("success overlay is visible").isTrue();
        assertThat(overlay.getOpacity()).as("success overlay opacity").isEqualTo(1);
        Node reviewData = overlay.getParent().getChildrenUnmodifiable().get(0);
        assertThat(reviewData.getEffect()).as("review data behind the success overlay is blurred")
                .isInstanceOf(GaussianBlur.class);
        assertThat(isShowing(button(robot, createOfferSuccessButtonText()))).isTrue();
    }

    private static Node createOfferSuccessOverlay(FxRobot robot) {
        String headline = Res.get("bisqEasy.tradeWizard.review.createOfferSuccess.headline");
        Node node = robot.lookup(n -> n instanceof Label label && headline.equals(label.getText())).query();
        while (!node.getStyleClass().contains("wizard-overlay")) {
            node = node.getParent();
        }
        return node;
    }

    private static String createOfferSuccessButtonText() {
        return Res.get("bisqEasy.tradeWizard.review.createOfferSuccessButton");
    }

    private static Button button(FxRobot robot, String text) {
        return robot.lookup(".button").queryAllAs(Button.class).stream()
                .filter(button -> text.equals(button.getText()))
                .filter(TradeWizardCreateOfferUiTest::isShowing)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No visible button '" + text + "'"));
    }

    private static void waitUntilShowing(FxRobot robot, String buttonText) throws TimeoutException {
        WaitForAsyncUtils.waitFor(STEP_TIMEOUT_SECONDS, TimeUnit.SECONDS, () ->
                robot.lookup(".button").queryAllAs(Button.class).stream()
                        .anyMatch(button -> buttonText.equals(button.getText()) && isShowing(button)));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private static boolean isShowing(Node node) {
        for (Node n = node; n != null; n = n.getParent()) {
            if (!n.isVisible() || n.getOpacity() == 0) {
                return false;
            }
        }
        return node.getScene() != null;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
