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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.trade_state;

import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.common.data.Triple;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.support.mediation.MediationService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.user.profile.UserProfile;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.Optional;

@Slf4j
class TradePhaseBox {
    private final Controller controller;

    TradePhaseBox(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    View getView() {
        return controller.getView();
    }

    void setSelectedChannel(BisqEasyPrivateTradeChatChannel channel) {
        controller.setSelectedChannel(channel);
    }

    void setBisqEasyTrade(BisqEasyTrade bisqEasyTrade) {
        controller.setBisqEasyTrade(bisqEasyTrade);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final MediationService mediationService;
        private Pin bisqEasyTradeStatePin;

        private Controller(ServiceProvider serviceProvider) {
            mediationService = serviceProvider.getSupportService().getMediationService();

            model = new Model();
            view = new View(model, this);
        }

        private void setSelectedChannel(BisqEasyPrivateTradeChatChannel channel) {
            model.setSelectedChannel(channel);
        }

        private void setBisqEasyTrade(BisqEasyTrade bisqEasyTrade) {
            model.setBisqEasyTrade(bisqEasyTrade);

            boolean isBuyer = bisqEasyTrade.isBuyer();

            model.getPhase1Info().set(isBuyer ?
                    Res.get("bisqEasy.tradeState.phase.buyer.phase1").toUpperCase() :
                    Res.get("bisqEasy.tradeState.phase.seller.phase1").toUpperCase());
            model.getPhase2Info().set(isBuyer ?
                    Res.get("bisqEasy.tradeState.phase.buyer.phase2a").toUpperCase() :
                    Res.get("bisqEasy.tradeState.phase.seller.phase2a").toUpperCase());
            model.getPhase3Info().set(isBuyer ?
                    Res.get("bisqEasy.tradeState.phase.buyer.phase3a").toUpperCase() :
                    Res.get("bisqEasy.tradeState.phase.seller.phase3a").toUpperCase());
            model.getPhase4Info().set(isBuyer ?
                    Res.get("bisqEasy.tradeState.phase.buyer.phase4").toUpperCase() :
                    Res.get("bisqEasy.tradeState.phase.seller.phase4").toUpperCase());
            model.getPhase5Info().set(Res.get("bisqEasy.tradeState.phase.phase5").toUpperCase());

            if (bisqEasyTradeStatePin != null) {
                bisqEasyTradeStatePin.unbind();
            }
            bisqEasyTradeStatePin = bisqEasyTrade.tradeStateObservable().addObserver(state -> {
                UIThread.run(() -> {
                    switch (state) {
                        case INIT:
                            break;
                        case TAKER_SENT_TAKE_OFFER_REQUEST:
                        case MAKER_SENT_TAKE_OFFER_RESPONSE:
                        case TAKER_RECEIVED_TAKE_OFFER_RESPONSE:
                            model.getPhaseIndex().set(0);
                            break;

                        case SELLER_SENT_ACCOUNT_DATA:
                        case BUYER_RECEIVED_ACCOUNT_DATA:
                        case BUYER_SENT_FIAT_SENT_CONFIRMATION:
                        case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION:
                            model.getPhaseIndex().set(1);
                            break;
                        case BUYER_SENT_BTC_ADDRESS:
                        case SELLER_RECEIVED_BTC_ADDRESS:
                        case SELLER_CONFIRMED_FIAT_RECEIPT:
                        case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION:
                            model.getPhaseIndex().set(2);
                            break;

                        case SELLER_SENT_BTC_SENT_CONFIRMATION:
                        case BUYER_RECEIVED_BTC_SENT_CONFIRMATION:
                            model.getPhaseIndex().set(3);
                            break;

                        case BTC_CONFIRMED:
                            model.getPhaseIndex().set(4);
                            break;
                    }
                    int phaseIndex = model.getPhaseIndex().get();
                    model.getDisputeButtonVisible().set(phaseIndex == 2 || phaseIndex == 3);

                    if (state.ordinal() >= BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION.ordinal()) {
                        model.getPhase2Info().set(isBuyer ?
                                Res.get("bisqEasy.tradeState.phase.buyer.phase2b").toUpperCase() :
                                Res.get("bisqEasy.tradeState.phase.seller.phase2b").toUpperCase());
                    }
                    if (state.ordinal() >= BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT.ordinal()) {
                        model.getPhase3Info().set(isBuyer ?
                                Res.get("bisqEasy.tradeState.phase.buyer.phase3b").toUpperCase() :
                                Res.get("bisqEasy.tradeState.phase.seller.phase3b").toUpperCase());
                    }
                });
            });
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        void onOpenTradeGuide() {
            Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
        }

        void onOpenDispute() {
            BisqEasyPrivateTradeChatChannel channel = model.getSelectedChannel();
            Optional<UserProfile> mediator = channel.getMediator();
            if (mediator.isPresent()) {
                new Popup().headLine(Res.get("bisqEasy.mediation.request.confirm.headline"))
                        .information(Res.get("bisqEasy.mediation.request.confirm.msg"))
                        .actionButtonText(Res.get("bisqEasy.mediation.request.confirm.openMediation"))
                        .onAction(() -> {
                            channel.setIsInMediation(true);
                            mediationService.requestMediation(channel);
                            new Popup().headLine(Res.get("bisqEasy.mediation.request.feedback.headline"))
                                    .feedback(Res.get("bisqEasy.mediation.request.feedback.msg")).show();
                        })
                        .closeButtonText(Res.get("action.cancel"))
                        .show();
            } else {
                new Popup().warning(Res.get("bisqEasy.mediation.request.feedback.noMediatorAvailable")).show();
            }
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private BisqEasyPrivateTradeChatChannel selectedChannel;
        @Setter
        private BisqEasyTrade bisqEasyTrade;

        private final IntegerProperty phaseIndex = new SimpleIntegerProperty();
        private final StringProperty phase1Info = new SimpleStringProperty();
        private final StringProperty phase2Info = new SimpleStringProperty();
        private final StringProperty phase3Info = new SimpleStringProperty();
        private final StringProperty phase4Info = new SimpleStringProperty();
        private final StringProperty phase5Info = new SimpleStringProperty();
        private final BooleanProperty disputeButtonVisible = new SimpleBooleanProperty();
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Label phase1Label, phase2Label, phase3Label, phase4Label, phase5Label;
        private final Button disputeButton;
        private final Hyperlink openTradeGuide;
        private final List<Triple<HBox, Label, Badge>> phaseItems;
        private Subscription phaseIndexPin;

        public View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setMinWidth(300);
            root.setMaxWidth(root.getMinWidth());

            Label phaseHeadline = new Label(Res.get("bisqEasy.tradeState.phase.headline"));
            phaseHeadline.getStyleClass().add("bisq-easy-trade-state-phase-headline");

            Triple<HBox, Label, Badge> phaseItem1 = getPhaseItem(1);
            Triple<HBox, Label, Badge> phaseItem2 = getPhaseItem(2);
            Triple<HBox, Label, Badge> phaseItem3 = getPhaseItem(3);
            Triple<HBox, Label, Badge> phaseItem4 = getPhaseItem(4);
            Triple<HBox, Label, Badge> phaseItem5 = getPhaseItem(5);

            HBox phase1HBox = phaseItem1.getFirst();
            HBox phase2HBox = phaseItem2.getFirst();
            HBox phase3HBox = phaseItem3.getFirst();
            HBox phase4HBox = phaseItem4.getFirst();
            HBox phase5HBox = phaseItem5.getFirst();

            phase1Label = phaseItem1.getSecond();
            phase2Label = phaseItem2.getSecond();
            phase3Label = phaseItem3.getSecond();
            phase4Label = phaseItem4.getSecond();
            phase5Label = phaseItem5.getSecond();

            phaseItems = List.of(phaseItem1, phaseItem2, phaseItem3, phaseItem4, phaseItem5);

            openTradeGuide = new Hyperlink(Res.get("bisqEasy.tradeState.openTradeGuide"));

            disputeButton = new Button(Res.get("bisqEasy.tradeState.openDispute"));
            disputeButton.getStyleClass().add("outlined-button");

            VBox.setMargin(phaseHeadline, new Insets(20, 0, 20, 0));
            VBox.setMargin(disputeButton, new Insets(15, 0, 0, 0));
            VBox.setMargin(openTradeGuide, new Insets(30, 0, 0, 2));

            root.getChildren().addAll(
                    phaseHeadline,
                    phase1HBox,
                    getVLine(),
                    phase2HBox,
                    getVLine(),
                    phase3HBox,
                    getVLine(),
                    phase4HBox,
                    getVLine(),
                    phase5HBox,
                    Spacer.fillVBox(),
                    openTradeGuide,
                    disputeButton);
        }

        @Override
        protected void onViewAttached() {
            phase1Label.textProperty().bind(model.getPhase1Info());
            phase2Label.textProperty().bind(model.getPhase2Info());
            phase3Label.textProperty().bind(model.getPhase3Info());
            phase4Label.textProperty().bind(model.getPhase4Info());
            phase5Label.textProperty().bind(model.getPhase5Info());
            disputeButton.visibleProperty().bind(model.getDisputeButtonVisible());
            disputeButton.managedProperty().bind(model.getDisputeButtonVisible());

            disputeButton.setOnAction(e -> controller.onOpenDispute());
            openTradeGuide.setOnAction(e -> controller.onOpenTradeGuide());
            phaseIndexPin = EasyBind.subscribe(model.getPhaseIndex(), this::phaseIndexChanged);
        }

        @Override
        protected void onViewDetached() {
            phase1Label.textProperty().unbind();
            phase2Label.textProperty().unbind();
            phase3Label.textProperty().unbind();
            phase4Label.textProperty().unbind();
            phase5Label.textProperty().unbind();
            disputeButton.visibleProperty().unbind();
            disputeButton.managedProperty().unbind();

            disputeButton.setOnAction(null);
            openTradeGuide.setOnAction(null);
            phaseIndexPin.unsubscribe();
        }

        private void phaseIndexChanged(Number phaseIndex) {
            for (int i = 0; i < phaseItems.size(); i++) {
                Badge badge = phaseItems.get(i).getThird();
                Label label = phaseItems.get(i).getSecond();
                badge.getStyleClass().clear();
                label.getStyleClass().clear();
                badge.getStyleClass().add("bisq-easy-trade-state-phase-badge");
                if (i <= phaseIndex.intValue()) {
                    badge.getStyleClass().add("bisq-easy-trade-state-phase-badge-active");
                    label.getStyleClass().add("bisq-easy-trade-state-phase-active");
                } else {
                    badge.getStyleClass().add("bisq-easy-trade-state-phase-badge-inactive");
                    label.getStyleClass().add("bisq-easy-trade-state-phase-inactive");
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
            badge.setText(String.valueOf(index));
            badge.setPrefSize(20, 20);
            HBox hBox = new HBox(7.5, badge, label);
            hBox.setAlignment(Pos.CENTER_LEFT);
            return new Triple<>(hBox, label, badge);
        }
    }
}