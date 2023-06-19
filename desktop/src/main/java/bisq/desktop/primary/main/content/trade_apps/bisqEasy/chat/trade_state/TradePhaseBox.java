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

package bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state;

import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.common.data.Triple;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.support.MediationService;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
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
public class TradePhaseBox {
    private final Controller controller;

    public TradePhaseBox(DefaultApplicationService applicationService) {
        controller = new Controller(applicationService);
    }

    public View getView() {
        return controller.getView();
    }

    public void setSelectedChannel(BisqEasyPrivateTradeChatChannel channel) {
        controller.setSelectedChannel(channel);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final MediationService mediationService;
        private final UserIdentityService userIdentityService;
        private final BisqEasyTradeService bisqEasyTradeService;
        private Pin bisqEasyTradeStatePin;

        private Controller(DefaultApplicationService applicationService) {
            mediationService = applicationService.getSupportService().getMediationService();
            userIdentityService = applicationService.getUserService().getUserIdentityService();
            bisqEasyTradeService = applicationService.getTradeService().getBisqEasyTradeService();

            model = new Model();
            view = new View(model, this);
        }

        public void setSelectedChannel(BisqEasyPrivateTradeChatChannel channel) {
            model.setSelectedChannel(channel);
            BisqEasyOffer bisqEasyOffer = channel.getBisqEasyOffer();
            UserIdentity myUserIdentity = channel.getMyUserIdentity();
            boolean maker = isMaker(bisqEasyOffer);
            NetworkId takerNetworkId = maker ?
                    channel.getPeer().getNetworkId() :
                    myUserIdentity.getUserProfile().getNetworkId();
            String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
            if (bisqEasyTradeService.findTrade(tradeId).isEmpty()) {
                log.error("###");
                return;
            }
            BisqEasyTrade bisqEasyTradeModel = bisqEasyTradeService.findTrade(tradeId).orElseThrow();
            model.setBisqEasyTradeModel(bisqEasyTradeModel);

            boolean isBuyer = bisqEasyTradeModel.isBuyer();

            model.getPhase1Info().set(isBuyer ? Res.get("bisqEasy.tradeState.phase.buyer.phase1").toUpperCase() :
                    Res.get("bisqEasy.tradeState.phase.seller.phase1").toUpperCase());
            model.getPhase2Info().set(isBuyer ? Res.get("bisqEasy.tradeState.phase.buyer.phase2").toUpperCase() :
                    Res.get("bisqEasy.tradeState.phase.seller.phase2").toUpperCase());
            model.getPhase3Info().set(isBuyer ? Res.get("bisqEasy.tradeState.phase.buyer.phase3").toUpperCase() :
                    Res.get("bisqEasy.tradeState.phase.seller.phase3").toUpperCase());
            model.getPhase4Info().set(isBuyer ? Res.get("bisqEasy.tradeState.phase.buyer.phase4").toUpperCase() :
                    Res.get("bisqEasy.tradeState.phase.seller.phase4").toUpperCase());
            model.getPhase5Info().set(Res.get("bisqEasy.tradeState.phase.phase5").toUpperCase());

           /* String directionString = isBuyer ?
                    Res.get("offer.buying").toUpperCase() :
                    Res.get("offer.selling").toUpperCase();
            AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
            String baseAmountString = OfferAmountFormatter.formatBaseSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
            model.getQuoteCode().set(bisqEasyOffer.getMarket().getQuoteCurrencyCode());
            model.getFormattedBaseAmount().set(baseAmountString);
            String quoteAmountString = OfferAmountFormatter.formatQuoteSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
            model.getFormattedQuoteAmount().set(quoteAmountString);
            FiatPaymentMethodSpec fiatPaymentMethodSpec = bisqEasyOffer.getQuoteSidePaymentMethodSpecs().get(0);
            String paymentMethodName = fiatPaymentMethodSpec.getPaymentMethod().getDisplayString();
            String tradeInfo = Res.get("bisqEasy.tradeState.header.headline",
                    directionString,
                    baseAmountString,
                    quoteAmountString,
                    paymentMethodName);

            model.getTradeInfo().set(tradeInfo);
            if (!isBuyer) {
                findUsersAccountData().ifPresent(accountData -> model.getSellersPaymentAccountData().set(accountData));
            }*/


            if (bisqEasyTradeStatePin != null) {
                bisqEasyTradeStatePin.unbind();
            }
            bisqEasyTradeStatePin = bisqEasyTradeModel.tradeStateObservable().addObserver(state -> {
                UIThread.run(() -> {
                    switch (state) {
                        case INIT:
                            break;
                        case TAKER_SEND_TAKE_OFFER_REQUEST:
                        case MAKER_RECEIVED_TAKE_OFFER_REQUEST:
                            model.getPhaseIndex().set(0);
                            break;
                        case SELLER_SENT_ACCOUNT_DATA:
                            model.getPhaseIndex().set(1);
                            break;
                        case BUYER_RECEIVED_ACCOUNT_DATA:
                            model.getPhaseIndex().set(1);
                            break;
                        case BUYER_SENT_FIAT_SENT_CONFIRMATION:
                            model.getPhaseIndex().set(2);
                            break;
                        case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION:
                            model.getPhaseIndex().set(2);
                            break;
                        case SELLER_SENT_BTC_SENT_CONFIRMATION:
                            model.getPhaseIndex().set(3);
                            break;
                        case BUYER_RECEIVED_BTC_SENT_CONFIRMATION:
                            model.getPhaseIndex().set(3);
                            break;
                        case BTC_CONFIRMED:
                            model.getPhaseIndex().set(4);
                            break;
                        case COMPLETED:
                            //todo
                            break;
                    }
                    int phaseIndex = model.getPhaseIndex().get();
                    model.getOpenDisputeButtonVisible().set(phaseIndex == 2 || phaseIndex == 3);
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

        private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
            return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private BisqEasyPrivateTradeChatChannel selectedChannel;
        @Setter
        private BisqEasyTrade bisqEasyTradeModel;

        private final StringProperty phase1Info = new SimpleStringProperty();
        private final StringProperty phase2Info = new SimpleStringProperty();
        private final StringProperty phase3Info = new SimpleStringProperty();
        private final StringProperty phase4Info = new SimpleStringProperty();
        private final StringProperty phase5Info = new SimpleStringProperty();
        private final BooleanProperty openDisputeButtonVisible = new SimpleBooleanProperty();
        private final IntegerProperty phaseIndex = new SimpleIntegerProperty();
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Label phase1Label, phase2Label, phase3Label, phase4Label, phase5Label;
        private final Button openDisputeButton;
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

            openDisputeButton = new Button(Res.get("bisqEasy.tradeState.openDispute"));
            openDisputeButton.getStyleClass().add("outlined-button");

            Region separator = Layout.hLine();

            VBox.setMargin(phaseHeadline, new Insets(20, 0, 20, 0));
            VBox.setMargin(openDisputeButton, new Insets(10, 0, 0, 0));
            VBox.setMargin(openTradeGuide, new Insets(30, 0, 0, 2));

            root.getChildren().addAll(separator,
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
                    openDisputeButton);
        }

        @Override
        protected void onViewAttached() {
            phase1Label.textProperty().bind(model.getPhase1Info());
            phase2Label.textProperty().bind(model.getPhase2Info());
            phase3Label.textProperty().bind(model.getPhase3Info());
            phase4Label.textProperty().bind(model.getPhase4Info());
            phase5Label.textProperty().bind(model.getPhase5Info());
            openDisputeButton.visibleProperty().bind(model.getOpenDisputeButtonVisible());
            openDisputeButton.managedProperty().bind(model.getOpenDisputeButtonVisible());

            openDisputeButton.setOnAction(e -> controller.onOpenDispute());
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
            openDisputeButton.visibleProperty().unbind();
            openDisputeButton.managedProperty().unbind();
            openDisputeButton.setOnAction(null);
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
            VBox.setMargin(separator, new Insets(5, 0, 5, 10));
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