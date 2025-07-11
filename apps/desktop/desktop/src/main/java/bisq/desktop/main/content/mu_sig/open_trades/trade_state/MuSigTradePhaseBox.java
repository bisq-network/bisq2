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

package bisq.desktop.main.content.mu_sig.open_trades.trade_state;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.data.Triple;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Layout;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.mu_sig.MuSigTrade;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.List;

@Slf4j
class MuSigTradePhaseBox {
    private final Controller controller;

    MuSigTradePhaseBox(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    VBox getRoot() {
        return controller.getView().getRoot();
    }

    void setSelectedChannel(@Nullable MuSigOpenTradeChannel channel) {
        controller.setSelectedChannel(channel);
    }

    void setMuSigTrade(MuSigTrade trade) {
        controller.setMuSigTrade(trade);
    }

    void reset() {
        controller.model.reset();
    }

    int getPhaseIndex() {
        return controller.model.getPhaseIndex().get();
    }

    public static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final MediationRequestService mediationRequestService;
        private final MuSigOpenTradeChannelService channelService;
        private Pin muSigTradeStatePin, isInMediationPin;

        private Controller(ServiceProvider serviceProvider) {
            mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();
            channelService = serviceProvider.getChatService().getMuSigOpenTradeChannelService();

            model = new Model();
            view = new View(model, this);
        }

        private void setSelectedChannel(@Nullable MuSigOpenTradeChannel channel) {
            model.setSelectedChannel(channel);
            if (isInMediationPin != null) {
                isInMediationPin.unbind();
                isInMediationPin = null;
            }
            if (channel != null) {
                isInMediationPin = FxBindings.bind(model.getIsInMediation()).to(channel.isInMediationObservable());
            }
        }

        private void setMuSigTrade(MuSigTrade trade) {
            model.setTrade(trade);
            if (muSigTradeStatePin != null) {
                muSigTradeStatePin.unbind();
                muSigTradeStatePin = null;
            }
            if (trade == null) {
                return;
            }

            model.getPhase1Info().set(Res.get("muSig.tradeState.phase1").toUpperCase());
            model.getPhase2Info().set(Res.get("muSig.tradeState.phase2").toUpperCase());
            model.getPhase3Info().set(Res.get("muSig.tradeState.phase3").toUpperCase());
            model.getPhase4Info().set(Res.get("muSig.tradeState.phase4").toUpperCase());


            muSigTradeStatePin = trade.tradeStateObservable().addObserver(state -> UIThread.run(() -> {
                model.getRequestMediationButtonVisible().set(!state.isFinalState());

                switch (state) {
                    case INIT,
                         TAKER_INITIALIZED_TRADE,
                         MAKER_INITIALIZED_TRADE_AND_CREATED_NONCE_SHARES,
                         TAKER_CREATED_NONCE_SHARES_AND_PARTIAL_SIGNATURES,
                         MAKER_CREATED_PARTIAL_SIGNATURES_AND_SIGNED_DEPOSIT_TX,
                         TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX,
                         MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX,
                         TAKER_RECEIVED_ACCOUNT_PAYLOAD-> {
                        model.getPhaseIndex().set(0);
                    }
                    case DEPOSIT_TX_CONFIRMED -> {
                        model.getPhaseIndex().set(1);
                    }
                    case BUYER_INITIATED_PAYMENT -> {
                        model.getPhaseIndex().set(2);
                    }
                    case SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE,
                         SELLER_CONFIRMED_PAYMENT_RECEIPT -> {
                        model.getPhaseIndex().set(2);
                    }
                    case BUYER_CLOSED_TRADE,
                         SELLER_CLOSED_TRADE,
                         BUYER_FORCE_CLOSED_TRADE,
                         SELLER_FORCE_CLOSED_TRADE -> {
                        model.getPhaseIndex().set(3);
                    }
                }
            }));
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
            if (isInMediationPin != null) {
                isInMediationPin.unbind();
                isInMediationPin = null;
            }

            if (muSigTradeStatePin != null) {
                muSigTradeStatePin.unbind();
                muSigTradeStatePin = null;
            }
        }

        void onOpenTradeGuide() {
            Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
        }

        void onOpenWalletHelp() {
            Navigation.navigateTo(NavigationTarget.WALLET_GUIDE);
        }

        void onRequestMediation() {
            MuSigOpenTradesUtils.requestMediation(model.getSelectedChannel(),
                    model.getTrade().getContract(),
                    mediationRequestService, channelService);
        }
    }

    @Getter
    public static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private MuSigOpenTradeChannel selectedChannel;
        @Setter
        private MuSigTrade trade;
        private final BooleanProperty requestMediationButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty isInMediation = new SimpleBooleanProperty();
        private final IntegerProperty phaseIndex = new SimpleIntegerProperty();
        private final StringProperty phase1Info = new SimpleStringProperty();
        private final StringProperty phase2Info = new SimpleStringProperty();
        private final StringProperty phase3Info = new SimpleStringProperty();
        private final StringProperty phase4Info = new SimpleStringProperty();

        void reset() {
            selectedChannel = null;
            trade = null;
            requestMediationButtonVisible.set(false);
            isInMediation.set(false);
            phaseIndex.set(0);
            phase1Info.set(null);
            phase2Info.set(null);
            phase3Info.set(null);
            phase4Info.set(null);
        }
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Label phase1Label, phase2Label, phase3Label, phase4Label;
        private final Button requestMediationButton;
        private final BisqMenuItem openTradeGuide, walletHelp;
        private final List<Triple<HBox, Label, Badge>> phaseItems;
        private Subscription phaseIndexPin;

        public View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setMinWidth(300);
            root.setMaxWidth(root.getMinWidth());

            Triple<HBox, Label, Badge> phaseItem1 = getPhaseItem(1);
            Triple<HBox, Label, Badge> phaseItem2 = getPhaseItem(2);
            Triple<HBox, Label, Badge> phaseItem3 = getPhaseItem(3);
            Triple<HBox, Label, Badge> phaseItem4 = getPhaseItem(4);

            HBox phase1HBox = phaseItem1.getFirst();
            HBox phase2HBox = phaseItem2.getFirst();
            HBox phase3HBox = phaseItem3.getFirst();
            HBox phase4HBox = phaseItem4.getFirst();

            phase1Label = phaseItem1.getSecond();
            phase2Label = phaseItem2.getSecond();
            phase3Label = phaseItem3.getSecond();
            phase4Label = phaseItem4.getSecond();

            phaseItems = List.of(phaseItem1, phaseItem2, phaseItem3, phaseItem4);

            double width = 160;
            walletHelp = new BisqMenuItem("icon-wallet", "icon-wallet-white", Res.get("bisqEasy.walletGuide.open"));
            walletHelp.setPrefWidth(width);
            openTradeGuide = new BisqMenuItem("trade-guide-grey", "trade-guide-white", Res.get("bisqEasy.tradeGuide.open"));
            openTradeGuide.setPrefWidth(width);
            VBox tradeOptionsVBox = new VBox(10, walletHelp, openTradeGuide);
            tradeOptionsVBox.setPadding(new Insets(0, 20, 0, 0));

            requestMediationButton = new Button(Res.get("bisqEasy.tradeState.requestMediation"));
            requestMediationButton.getStyleClass().addAll("outlined-button", "request-mediation-button");

            VBox.setMargin(phase1HBox, new Insets(25, 0, 0, 0));
            VBox.setMargin(tradeOptionsVBox, new Insets(30, 0, 0, 0));
            VBox.setMargin(requestMediationButton, new Insets(20, 0, 0, 0));
            root.getChildren().addAll(
                    phase1HBox,
                    getVLine(),
                    phase2HBox,
                    getVLine(),
                    phase3HBox,
                    getVLine(),
                    phase4HBox,
                    Spacer.fillVBox(),
                    tradeOptionsVBox,
                    requestMediationButton);
        }

        @Override
        protected void onViewAttached() {
            phase1Label.textProperty().bind(model.getPhase1Info());
            phase2Label.textProperty().bind(model.getPhase2Info());
            phase3Label.textProperty().bind(model.getPhase3Info());
            phase4Label.textProperty().bind(model.getPhase4Info());
            requestMediationButton.visibleProperty().bind(model.getRequestMediationButtonVisible());
            requestMediationButton.managedProperty().bind(model.getRequestMediationButtonVisible());
            requestMediationButton.disableProperty().bind(model.getIsInMediation());

            requestMediationButton.setOnAction(e -> controller.onRequestMediation());
            openTradeGuide.setOnAction(e -> controller.onOpenTradeGuide());
            walletHelp.setOnAction(e -> controller.onOpenWalletHelp());
            phaseIndexPin = EasyBind.subscribe(model.getPhaseIndex(), this::phaseIndexChanged);
        }

        @Override
        protected void onViewDetached() {
            phase1Label.textProperty().unbind();
            phase2Label.textProperty().unbind();
            phase3Label.textProperty().unbind();
            phase4Label.textProperty().unbind();
            requestMediationButton.visibleProperty().unbind();
            requestMediationButton.managedProperty().unbind();
            requestMediationButton.disableProperty().unbind();

            requestMediationButton.setOnAction(null);
            walletHelp.setOnAction(null);
            openTradeGuide.setOnAction(null);

            phaseIndexPin.unsubscribe();
        }

        private void phaseIndexChanged(Number phaseIndex) {
            for (int index = 0; index < phaseItems.size(); index++) {
                Badge badge = phaseItems.get(index).getThird();
                badge.getStyleClass().clear();
                badge.getStyleClass().add("bisq-easy-trade-state-phase-badge");

                Label label = phaseItems.get(index).getSecond();
                label.getStyleClass().clear();

                if (index < phaseIndex.intValue()) {
                    // completed
                    ImageView checkMark = ImageUtil.getImageViewById("check-white");
                    checkMark.setOpacity(0.75);
                    badge.getLabel().setGraphic(checkMark);
                    badge.setText("");
                    badge.setLabelStyle(null);
                    badge.getStyleClass().add("bisq-easy-trade-state-phase-badge-completed");
                    label.getStyleClass().add("bisq-easy-trade-state-phase-completed");
                } else {
                    badge.setText(String.valueOf(index + 1));
                    badge.getLabel().setGraphic(null);
                    if (index == phaseIndex.intValue()) {
                        // current
                        badge.setLabelStyle("-fx-text-fill: white; -fx-font-family: \"IBM Plex Sans Bold\"");
                        badge.getStyleClass().add("bisq-easy-trade-state-phase-badge-current");
                        label.getStyleClass().add("bisq-easy-trade-state-phase-current");
                    } else {
                        // open
                        badge.setLabelStyle("-fx-text-fill: -fx-mid-text-color; -fx-font-family: \"IBM Plex Sans\"");
                        badge.getStyleClass().add("bisq-easy-trade-state-phase-badge-open");
                        label.getStyleClass().add("bisq-easy-trade-state-phase-open");
                    }
                }
            }
        }

        private static Region getVLine() {
            Region separator = Layout.vLine();
            separator.setMinHeight(10);
            separator.setMaxHeight(separator.getMinHeight());
            VBox.setMargin(separator, new Insets(5, 0, 5, 12));
            return separator;
        }

        private static Triple<HBox, Label, Badge> getPhaseItem(int index) {
            Label label = new Label();
            label.getStyleClass().add("bisq-easy-trade-state-phase");
            Badge badge = new Badge();
            HBox hBox = new HBox(7.5, badge, label);
            hBox.setAlignment(Pos.CENTER_LEFT);
            return new Triple<>(hBox, label, badge);
        }
    }
}
