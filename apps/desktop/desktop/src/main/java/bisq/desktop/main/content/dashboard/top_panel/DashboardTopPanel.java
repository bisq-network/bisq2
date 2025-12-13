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

package bisq.desktop.main.content.dashboard.top_panel;

import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.data.Pair;
import bisq.common.data.Triple;
import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.presentation.formatters.PriceFormatter;
import bisq.user.profile.UserProfileService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DashboardTopPanel {
    private final DashboardTopPanel.Controller controller;

    public DashboardTopPanel(ServiceProvider serviceProvider) {
        controller = new DashboardTopPanel.Controller(serviceProvider);
    }

    public HBox getViewRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    public static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final MarketPriceService marketPriceService;
        private final Model model;
        private final UserProfileService userProfileService;
        private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
        private final BisqEasyOfferbookMessageService bisqEasyOfferbookMessageService;
        private Pin selectedMarketPin, marketPricePin, getNumUserProfilesPin;
        private final Set<Pin> channelsPins = new HashSet<>();
        private boolean allowUpdateOffersOnline;

        public Controller(ServiceProvider serviceProvider) {
            marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
            userProfileService = serviceProvider.getUserService().getUserProfileService();
            bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
            bisqEasyOfferbookMessageService = serviceProvider.getBisqEasyService().getBisqEasyOfferbookMessageService();

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            selectedMarketPin = marketPriceService.getSelectedMarket().addObserver(selectedMarket -> updateMarketPrice());
            marketPricePin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(this::updateMarketPrice);

            getNumUserProfilesPin = userProfileService.getNumUserProfiles().addObserver(numUserProfiles ->
                    UIThread.run(() -> model.getActiveUsers().set(String.valueOf(numUserProfiles))));

            channelsPins.addAll(bisqEasyOfferbookChannelService.getChannels().stream()
                    .map(channel -> channel.getChatMessages().addObserver(this::updateOffersOnline))
                    .collect(Collectors.toSet()));

            // We trigger a call of updateOffersOnline for each channel when registering our observer. But we only want one call,
            // so we block execution of the code inside updateOffersOnline to only call it once.
            allowUpdateOffersOnline = true;
            updateOffersOnline();
        }

        @Override
        public void onDeactivate() {
            selectedMarketPin.unbind();
            marketPricePin.unbind();
            getNumUserProfilesPin.unbind();
            channelsPins.forEach(Pin::unbind);
            channelsPins.clear();
        }

        public void onBuildReputation() {
            Navigation.navigateTo(NavigationTarget.BUILD_REPUTATION);
        }

        public void onOpenTradeOverview() {
            Navigation.navigateTo(NavigationTarget.TRADE_PROTOCOLS);
        }

        public void onOpenBisqEasy() {
            Navigation.navigateTo(NavigationTarget.BISQ_EASY);
        }

        private void updateMarketPrice() {
            Market selectedMarket = marketPriceService.getSelectedMarket().get();
            if (selectedMarket != null) {
                marketPriceService.findMarketPrice(selectedMarket)
                        .ifPresent(marketPrice -> UIThread.run(() -> {
                            model.getMarketPrice().set(PriceFormatter.format(marketPrice.getPriceQuote(), true));
                            model.getMarketCode().set(marketPrice.getMarket().getMarketCodes());
                        }));
            }
        }

        private void updateOffersOnline() {
            if (allowUpdateOffersOnline) {
                UIThread.run(() -> {
                    long numOffers = bisqEasyOfferbookMessageService.getAllOffers().count();
                    model.getOffersOnline().set(String.valueOf(numOffers));
                });
            }
        }
    }

    @Getter
    public static class Model implements bisq.desktop.common.view.Model {
        private final StringProperty marketPrice = new SimpleStringProperty();
        private final StringProperty marketCode = new SimpleStringProperty();
        private final StringProperty offersOnline = new SimpleStringProperty();
        private final StringProperty activeUsers = new SimpleStringProperty();
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Label marketPriceLabel, marketCodeLabel, offersOnlineLabel, activeUsersLabel;
        private final HBox marketPriceHBox;
        private Subscription marketPricePin;

        public View(Model model, Controller controller) {
            super(new HBox(16), model, controller);

            root.getStyleClass().add("bisq-box-2");
            root.setPadding(new Insets(20, 40, 20, 40));

            Triple<Pair<HBox, VBox>, Label, Label> priceTriple = getPriceBox(Res.get("dashboard.marketPrice"));
            marketPriceHBox = priceTriple.getFirst().getFirst();
            VBox marketPrice = priceTriple.getFirst().getSecond();
            marketPriceLabel = priceTriple.getSecond();
            marketCodeLabel = priceTriple.getThird();

            Pair<VBox, Label> offersPair = getValueBox(Res.get("dashboard.offersOnline"));
            VBox offersOnline = offersPair.getFirst();
            offersOnlineLabel = offersPair.getSecond();

            Pair<VBox, Label> usersPair = getValueBox(Res.get("dashboard.activeUsers"),
                    Optional.of(Res.get("dashboard.activeUsers.tooltip")));
            VBox activeUsers = usersPair.getFirst();
            activeUsersLabel = usersPair.getSecond();

            root.getChildren().addAll(marketPrice, offersOnline, activeUsers);
        }

        @Override
        protected void onViewAttached() {
            marketPriceLabel.textProperty().bind(model.getMarketPrice());
            marketCodeLabel.textProperty().bind(model.getMarketCode());
            offersOnlineLabel.textProperty().bind(model.getOffersOnline());
            activeUsersLabel.textProperty().bind(model.getActiveUsers());

            marketPricePin = EasyBind.subscribe(model.getMarketPrice(), value -> {
                if (value != null) {
                    double standardLength = 9; // USD
                    int length = value.length();
                    double ratio = Math.min(1, standardLength / length);

                    double priceFontSize = 3.4 * ratio;
                    marketPriceLabel.setStyle("-fx-font-size: " + priceFontSize + "em;");

                    double codeFontSize = 2.0 * ratio;
                    marketCodeLabel.setStyle("-fx-font-size: " + codeFontSize + "em;");

                    double hBoxTop = 50 - (50 * ratio);
                    VBox.setMargin(marketPriceHBox, new Insets(hBoxTop, 0, 0, 0));
                }
            });
        }

        @Override
        protected void onViewDetached() {
            marketPriceLabel.textProperty().unbind();
            marketCodeLabel.textProperty().unbind();
            offersOnlineLabel.textProperty().unbind();
            activeUsersLabel.textProperty().unbind();

            marketPricePin.unsubscribe();
        }

        private Triple<Pair<HBox, VBox>, Label, Label> getPriceBox(String title) {
            Label titleLabel = new Label(title);
            titleLabel.setTextAlignment(TextAlignment.CENTER);
            titleLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9");

            Label valueLabel = new Label();
            valueLabel.getStyleClass().add("bisq-text-headline-3");

            Label codeLabel = new Label();
            codeLabel.getStyleClass().addAll("bisq-text-12");

            HBox hBox = new HBox(9, valueLabel, codeLabel);
            hBox.setAlignment(Pos.BASELINE_CENTER);
            VBox.setMargin(titleLabel, new Insets(0, 80, 0, 0));
            VBox vBox = new VBox(titleLabel, hBox);
            vBox.setAlignment(Pos.TOP_CENTER);
            HBox.setHgrow(vBox, Priority.ALWAYS);
            return new Triple<>(new Pair<>(hBox, vBox), valueLabel, codeLabel);
        }

        private Pair<VBox, Label> getValueBox(String title) {
            return getValueBox(title, Optional.empty());
        }

        private Pair<VBox, Label> getValueBox(String title, Optional<String> tooltipText) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9");
            HBox titleHBox = new HBox(titleLabel);
            titleHBox.setAlignment(Pos.CENTER);

            if (tooltipText.isPresent()) {
                ImageView tooltipIcon = ImageUtil.getImageViewById("info");
                tooltipIcon.setOpacity(0.6);
                BisqTooltip tooltip = new BisqTooltip(tooltipText.get());
                Tooltip.install(tooltipIcon, tooltip);
                titleHBox.getChildren().add(tooltipIcon);
                titleHBox.setSpacing(5);
            }

            Label valueLabel = new Label();
            valueLabel.getStyleClass().add("bisq-text-headline-3");

            VBox box = new VBox(titleHBox, valueLabel);
            box.setAlignment(Pos.TOP_CENTER);
            HBox.setHgrow(box, Priority.ALWAYS);
            return new Pair<>(box, valueLabel);
        }
    }
}
