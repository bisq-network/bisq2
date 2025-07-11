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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
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
import bisq.i18n.Res;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import javafx.beans.property.*;
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
class TradePhaseBox {
    private final Controller controller;

    TradePhaseBox(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    View getView() {
        return controller.getView();
    }

    void setSelectedChannel(@Nullable BisqEasyOpenTradeChannel channel) {
        controller.setSelectedChannel(channel);
    }

    void setBisqEasyTrade(BisqEasyTrade bisqEasyTrade) {
        controller.setBisqEasyTrade(bisqEasyTrade);
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
        private final BisqEasyOpenTradeChannelService channelService;
        private Pin bisqEasyTradeStatePin, isInMediationPin;

        private Controller(ServiceProvider serviceProvider) {
            mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();
            channelService = serviceProvider.getChatService().getBisqEasyOpenTradeChannelService();

            model = new Model();
            view = new View(model, this);
        }

        private void setSelectedChannel(@Nullable BisqEasyOpenTradeChannel channel) {
            model.setSelectedChannel(channel);
            if (isInMediationPin != null) {
                isInMediationPin.unbind();
            }
            if (channel != null) {
                isInMediationPin = FxBindings.bind(model.getIsInMediation()).to(channel.isInMediationObservable());
            }
        }

        private void setBisqEasyTrade(BisqEasyTrade bisqEasyTrade) {
            model.setBisqEasyTrade(bisqEasyTrade);
            if (bisqEasyTradeStatePin != null) {
                bisqEasyTradeStatePin.unbind();
            }
            if (bisqEasyTrade == null) {
                return;
            }

            model.getPhase1Info().set(Res.get("bisqEasy.tradeState.phase1").toUpperCase());
            model.getPhase2Info().set(Res.get("bisqEasy.tradeState.phase2").toUpperCase());
            model.getPhase3Info().set(Res.get("bisqEasy.tradeState.phase3").toUpperCase());
            model.getPhase4Info().set(Res.get("bisqEasy.tradeState.phase4").toUpperCase());

            bisqEasyTradeStatePin = bisqEasyTrade.tradeStateObservable().addObserver(state -> UIThread.run(() -> {
                switch (state) {
                    case INIT:
                        break;

                    case TAKER_SENT_TAKE_OFFER_REQUEST:

                        // Seller
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_:
                    case MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
                        // Buyer
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_:
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
                    case TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
                    case MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_:
                        model.getPhaseIndex().set(0);
                        model.getRequestMediationButtonVisible().set(false);
                        model.getReportToMediatorButtonVisible().set(true);
                        break;

                    // Seller
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_:
                    case TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS:
                    case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION:
                        // Buyer
                    case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
                    case MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA:
                    case BUYER_SENT_FIAT_SENT_CONFIRMATION:
                        model.getPhaseIndex().set(1);
                        boolean showRequestMediationButton =
                                state == BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION
                                        || state == BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION;
                        model.getRequestMediationButtonVisible().set(showRequestMediationButton);
                        model.getReportToMediatorButtonVisible().set(!showRequestMediationButton);
                        break;

                    case SELLER_CONFIRMED_FIAT_RECEIPT:
                    case SELLER_SENT_BTC_SENT_CONFIRMATION:
                    case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION:
                    case BUYER_RECEIVED_BTC_SENT_CONFIRMATION:
                        model.getPhaseIndex().set(2);
                        model.getRequestMediationButtonVisible().set(true);
                        model.getReportToMediatorButtonVisible().set(false);
                        break;

                    case BTC_CONFIRMED:
                        model.getPhaseIndex().set(3);
                        model.getRequestMediationButtonVisible().set(false);
                        model.getReportToMediatorButtonVisible().set(true);
                        break;

                    case REJECTED:
                    case PEER_REJECTED:
                    case CANCELLED:
                    case PEER_CANCELLED:
                        model.getRequestMediationButtonVisible().set(false);
                        model.getReportToMediatorButtonVisible().set(true);
                        break;

                    case FAILED:
                    case FAILED_AT_PEER:
                        model.getRequestMediationButtonVisible().set(false);
                        model.getReportToMediatorButtonVisible().set(false);
                        break;

                    default:
                        log.error("State {} not handled", state.name());
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
            }
        }

        void onOpenTradeGuide() {
            Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
        }

        void onOpenWalletHelp() {
            Navigation.navigateTo(NavigationTarget.WALLET_GUIDE);
        }

        void onRequestMediation() {
            OpenTradesUtils.requestMediation(model.getSelectedChannel(),
                    model.getBisqEasyTrade().getContract(),
                    mediationRequestService, channelService);
        }
    }

    @Getter
    public static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private BisqEasyOpenTradeChannel selectedChannel;
        @Setter
        private BisqEasyTrade bisqEasyTrade;
        private final BooleanProperty requestMediationButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty reportToMediatorButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty isInMediation = new SimpleBooleanProperty();
        private final IntegerProperty phaseIndex = new SimpleIntegerProperty();
        private final StringProperty phase1Info = new SimpleStringProperty();
        private final StringProperty phase2Info = new SimpleStringProperty();
        private final StringProperty phase3Info = new SimpleStringProperty();
        private final StringProperty phase4Info = new SimpleStringProperty();

        void reset() {
            selectedChannel = null;
            bisqEasyTrade = null;
            requestMediationButtonVisible.set(false);
            reportToMediatorButtonVisible.set(false);
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
        private final BisqMenuItem openTradeGuide, walletHelp, reportToMediator;
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
            reportToMediator = new BisqMenuItem("icon-report", "icon-report-white", Res.get("bisqEasy.tradeState.reportToMediator"));
            reportToMediator.setPrefWidth(width);
            VBox tradeOptionsVBox = new VBox(10, walletHelp, openTradeGuide, reportToMediator);
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
            reportToMediator.visibleProperty().bind(model.getReportToMediatorButtonVisible().and(model.getIsInMediation().not()));
            reportToMediator.managedProperty().bind(reportToMediator.visibleProperty());
            requestMediationButton.visibleProperty().bind(model.getRequestMediationButtonVisible());
            requestMediationButton.managedProperty().bind(model.getRequestMediationButtonVisible());
            requestMediationButton.disableProperty().bind(model.getIsInMediation());

            requestMediationButton.setOnAction(e -> controller.onRequestMediation());
            reportToMediator.setOnAction(e -> controller.onRequestMediation());
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
            reportToMediator.visibleProperty().unbind();
            reportToMediator.managedProperty().unbind();
            requestMediationButton.visibleProperty().unbind();
            requestMediationButton.managedProperty().unbind();
            requestMediationButton.disableProperty().unbind();

            requestMediationButton.setOnAction(null);
            reportToMediator.setOnAction(null);
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
            badge.setUseAnimation(false);
            HBox hBox = new HBox(7.5, badge, label);
            hBox.setAlignment(Pos.CENTER_LEFT);
            return new Triple<>(hBox, label, badge);
        }
    }
}
