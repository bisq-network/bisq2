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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig.components;

import bisq.account.accounts.AccountPayload;
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.MuSigMediationCaseListItem;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.common.observable.Pin;
import bisq.support.mediation.MediationCaseState;
import bisq.support.mediation.mu_sig.MuSigMediationCase;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import bisq.support.mediation.mu_sig.MuSigMediatorService;
import bisq.trade.mu_sig.MuSigTradeFormatter;
import bisq.trade.mu_sig.MuSigTradeUtils;
import bisq.user.profile.UserProfile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

import static bisq.desktop.components.helpers.LabeledValueRowFactory.createAndGetDescriptionAndValueBox;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getCopyButton;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getDescriptionLabel;
import static bisq.desktop.components.helpers.LabeledValueRowFactory.getValueLabel;

public class MuSigMediationCaseOverviewSection {

    private final Controller controller;

    public MuSigMediationCaseOverviewSection(ServiceProvider serviceProvider, boolean isCompactView) {
        this.controller = new Controller(serviceProvider, isCompactView);
    }

    public VBox getRoot() {
        return controller.view.getRoot();
    }

    public void setMediationCaseListItem(MuSigMediationCaseListItem item) {
        controller.setMediationCaseListItem(item);
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {

        @Getter
        private final View view;
        private final Model model;

        private final MuSigMediatorService muSigMediatorService;
        private Pin takerPaymentAccountDataPin;
        private Pin makerPaymentAccountDataPin;

        private Controller(ServiceProvider serviceProvider, boolean isCompactView) {
            model = new Model();
            model.setCompactView(isCompactView);
            view = new View(new VBox(), model, this);
            muSigMediatorService = serviceProvider.getSupportService().getMuSigMediatorService();
        }

        private void setMediationCaseListItem(MuSigMediationCaseListItem item) {
            model.setMuSigMediationCaseListItem(item);
        }

        @Override
        public void onActivate() {
            MuSigMediationCaseListItem muSigMediationCaseListItem = model.getMuSigMediationCaseListItem();
            MuSigMediationCase muSigMediationCase = muSigMediationCaseListItem.getMuSigMediationCase();
            MuSigMediationRequest muSigMediationRequest = muSigMediationCase.getMuSigMediationRequest();
            MuSigContract contract = muSigMediationRequest.getContract();
            MuSigOffer offer = contract.getOffer();

            MuSigMediationCaseListItem.Trader maker = muSigMediationCaseListItem.getMaker();
            MuSigMediationCaseListItem.Trader taker = muSigMediationCaseListItem.getTaker();
            Direction displayDirection = offer.getDisplayDirection();
            MuSigMediationCaseListItem.Trader buyer = displayDirection.isBuy() ? maker : taker;
            MuSigMediationCaseListItem.Trader seller = displayDirection.isSell() ? maker : taker;

            model.setBuyerUserName(formatUserNameWithMakerTakerRole(buyer, maker));
            model.setSellerUserName(formatUserNameWithMakerTakerRole(seller, maker));
            model.setBuyerBotId(buyer.getUserProfile().getNym());
            model.setBuyerUserId(buyer.getUserProfile().getId());

            CaseCounts buyerCaseCounts = getCaseCounts(buyer.getUserProfile());
            CaseCounts sellerCaseCounts = getCaseCounts(seller.getUserProfile());

            model.setBuyerCaseCountTotal(buyerCaseCounts.total());
            model.setBuyerCaseCountOpen(buyerCaseCounts.open());
            model.setBuyerCaseCountClosed(buyerCaseCounts.closed());
            model.setSellerBotId(seller.getUserProfile().getNym());
            model.setSellerUserId(seller.getUserProfile().getId());
            model.setSellerCaseCountTotal(sellerCaseCounts.total());
            model.setSellerCaseCountOpen(sellerCaseCounts.open());
            model.setSellerCaseCountClosed(sellerCaseCounts.closed());

            model.setNonBtcAmount(MuSigTradeFormatter.formatNonBtcSideAmount(contract));
            model.setNonBtcCurrency(offer.getMarket().getNonBtcCurrencyCode());
            model.setBtcAmount(MuSigTradeFormatter.formatBtcSideAmount(contract));
            model.setPrice(PriceFormatter.format(MuSigTradeUtils.getPriceQuote(contract)));
            model.setPriceCodes(offer.getMarket().getMarketCodes());
            model.setPriceSpec(offer.getPriceSpec() instanceof FixPriceSpec
                    ? ""
                    : String.format("(%s)", PriceSpecFormatter.getFormattedPriceSpec(offer.getPriceSpec(), true)));
            model.setPaymentMethod(contract.getNonBtcSidePaymentMethodSpec().getShortDisplayString());
            model.setPaymentMethodsBoxVisible(offer.getMarket().isBaseCurrencyBitcoin());
            model.getHasPaymentAccountData().set(false);
            model.getWaitingForPaymentAccountDataResponse().set(false);
            model.getTakerPaymentAccountData().set("");
            model.getMakerPaymentAccountData().set("");
            maybeAddPaymentAccountDataObservers();

            model.setDepositTxId(Res.get("bisqEasy.openTrades.tradeDetails.dataNotYetProvided"));
            model.setDepositTxIdEmpty(true);
        }

        private void requestPaymentAccountData() {
            MuSigMediationCaseListItem muSigMediationCaseListItem = model.getMuSigMediationCaseListItem();
            if (muSigMediationCaseListItem == null) {
                return;
            }
            model.getHasPaymentAccountData().set(false);
            model.getTakerPaymentAccountData().set("");
            model.getMakerPaymentAccountData().set("");
            boolean requestSent = muSigMediatorService.requestPaymentDetails(muSigMediationCaseListItem.getMuSigMediationCase());
            model.getWaitingForPaymentAccountDataResponse().set(requestSent);
        }

        @Override
        public void onDeactivate() {
            if (takerPaymentAccountDataPin != null) {
                takerPaymentAccountDataPin.unbind();
                takerPaymentAccountDataPin = null;
            }
            if (makerPaymentAccountDataPin != null) {
                makerPaymentAccountDataPin.unbind();
                makerPaymentAccountDataPin = null;
            }
        }

        private CaseCounts getCaseCounts(UserProfile userProfile) {
            var counts = muSigMediatorService.getMediationCases().stream()
                    .filter(mediationCase -> {
                        MuSigMediationRequest request = mediationCase.getMuSigMediationRequest();
                        return userProfile.equals(request.getRequester()) || userProfile.equals(request.getPeer());
                    })
                    .collect(Collectors.partitioningBy(mediationCase ->
                                    mediationCase.getMediationCaseState().get() == MediationCaseState.CLOSED,
                            Collectors.counting()));
            int closed = counts.getOrDefault(true, 0L).intValue();
            int open = counts.getOrDefault(false, 0L).intValue();
            return new CaseCounts(open + closed, open, closed);
        }

        private record CaseCounts(int total, int open, int closed) {
        }

        private String formatUserNameWithMakerTakerRole(MuSigMediationCaseListItem.Trader trader,
                                                        MuSigMediationCaseListItem.Trader maker) {
            boolean isMaker = trader.getUserProfile().getId().equals(maker.getUserProfile().getId());
            String makerTakerRole = MuSigTradeFormatter.getMakerTakerRole(isMaker);
            return String.format("%s (%s)",
                    trader.getUserName(),
                    makerTakerRole.toLowerCase());
        }

        private void maybeAddPaymentAccountDataObservers() {
            if (model.isCompactView()) {
                return;
            }
            if (takerPaymentAccountDataPin != null) {
                takerPaymentAccountDataPin.unbind();
                takerPaymentAccountDataPin = null;
            }
            if (makerPaymentAccountDataPin != null) {
                makerPaymentAccountDataPin.unbind();
                makerPaymentAccountDataPin = null;
            }

            MuSigMediationCaseListItem muSigMediationCaseListItem = model.getMuSigMediationCaseListItem();
            if (muSigMediationCaseListItem == null) {
                return;
            }

            MuSigMediationCase caseModel = muSigMediationCaseListItem.getMuSigMediationCase();
            takerPaymentAccountDataPin = caseModel.getTakerAccountPayload().addObserver(payload -> UIThread.run(() -> {
                model.getTakerPaymentAccountData().set(payload.map(AccountPayload::getAccountDataDisplayString).orElse(""));
                applyHasPaymentAccountDataState(caseModel);
            }));
            makerPaymentAccountDataPin = caseModel.getMakerAccountPayload().addObserver(payload -> UIThread.run(() -> {
                model.getMakerPaymentAccountData().set(payload.map(AccountPayload::getAccountDataDisplayString).orElse(""));
                applyHasPaymentAccountDataState(caseModel);
            }));
        }

        private void applyHasPaymentAccountDataState(MuSigMediationCase muSigMediationCase) {
            boolean hasBothPayloads = muSigMediationCase.getTakerAccountPayload().get().isPresent()
                    && muSigMediationCase.getMakerAccountPayload().get().isPresent();
            model.getHasPaymentAccountData().set(hasBothPayloads);
            if (hasBothPayloads) {
                model.getWaitingForPaymentAccountDataResponse().set(false);
            }
        }
    }

    @Slf4j
    @Getter
    @Setter
    private static class Model implements bisq.desktop.common.view.Model {
        private MuSigMediationCaseListItem muSigMediationCaseListItem;

        private boolean isCompactView;

        private String buyerUserName;
        private String sellerUserName;
        private String buyerBotId;
        private String buyerUserId;
        private int buyerCaseCountTotal;
        private int buyerCaseCountOpen;
        private int buyerCaseCountClosed;
        private String sellerBotId;
        private String sellerUserId;
        private int sellerCaseCountTotal;
        private int sellerCaseCountOpen;
        private int sellerCaseCountClosed;

        private String nonBtcAmount;
        private String nonBtcCurrency;
        private String btcAmount;
        private String price;
        private String priceCodes;
        private String priceSpec;

        private String paymentMethod;
        private boolean isPaymentMethodsBoxVisible;
        private final BooleanProperty hasPaymentAccountData = new SimpleBooleanProperty(false);
        private final BooleanProperty waitingForPaymentAccountDataResponse = new SimpleBooleanProperty(false);
        private final StringProperty takerPaymentAccountData = new SimpleStringProperty("");
        private final StringProperty makerPaymentAccountData = new SimpleStringProperty("");

        private String depositTxId;
        private boolean isDepositTxIdEmpty;

    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private final Label nonBtcAmountLabel, nonBtcCurrencyLabel, btcAmountLabel, priceLabel,
                priceCodesLabel, priceSpecLabel;
        private Label buyerUserNameLabel, sellerUserNameLabel, paymentMethodLabel, depositTxTitleLabel, depositTxDetailsLabel;
        private Label paymentAccountDataWaitingLabel, takerPaymentAccountDataLabel, makerPaymentAccountDataLabel;
        private BisqMenuItem buyerUserNameCopyButton, sellerUserNameCopyButton, depositTxCopyButton,
                paymentAccountDataRefreshButton;
        private HBox paymentMethodsBox, paymentAccountDataBox, takerPaymentAccountDataBox, makerPaymentAccountDataBox;

        public View(VBox root, Model model, Controller controller) {
            super(root, model, controller);

            // Amount and price
            nonBtcAmountLabel = getValueLabel();
            nonBtcCurrencyLabel = new Label();
            nonBtcCurrencyLabel.getStyleClass().addAll("text-fill-white", "small-text");

            Label openParenthesisLabel = new Label("(");
            openParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
            btcAmountLabel = getValueLabel();
            btcAmountLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
            btcAmountLabel.setPadding(new Insets(0, 5, 0, 0));
            Label btcLabel = new Label("BTC");
            btcLabel.getStyleClass().addAll("text-fill-grey-dimmed", "small-text");
            Label closingParenthesisLabel = new Label(")");
            closingParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
            HBox btcAmountHBox = new HBox(openParenthesisLabel, btcAmountLabel, btcLabel, closingParenthesisLabel);
            btcAmountHBox.setAlignment(Pos.BASELINE_LEFT);
            Label atLabel = new Label("@");
            atLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
            priceLabel = getValueLabel();
            priceCodesLabel = new Label();
            priceCodesLabel.getStyleClass().addAll("text-fill-white", "small-text");
            priceSpecLabel = new Label();
            priceSpecLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
            HBox amountAndPriceDetailsHBox = new HBox(5, nonBtcAmountLabel, nonBtcCurrencyLabel, btcAmountHBox,
                    atLabel, priceLabel, priceCodesLabel, priceSpecLabel);
            amountAndPriceDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
            HBox amountAndPriceBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.amountAndPrice", amountAndPriceDetailsHBox);

            VBox content;

            if (!model.isCompactView()) {
                // UserNames
                buyerUserNameLabel = getValueLabel();
                buyerUserNameCopyButton = getCopyButton(Res.get("authorizedRole.mediator.mediationCaseDetails.buyerUserName.copy"));
                HBox buyerUserNameBox = createAndGetDescriptionAndValueBox("authorizedRole.mediator.mediationCaseDetails.buyerUserName",
                        buyerUserNameLabel, buyerUserNameCopyButton);

                sellerUserNameLabel = getValueLabel();
                sellerUserNameCopyButton = getCopyButton(Res.get("authorizedRole.mediator.mediationCaseDetails.sellerUserName.copy"));
                HBox sellerUserNameBox = createAndGetDescriptionAndValueBox("authorizedRole.mediator.mediationCaseDetails.sellerUserName",
                        sellerUserNameLabel, sellerUserNameCopyButton);

                // Payment and settlement methods
                paymentMethodLabel = getValueLabel();
                HBox paymentMethodsDetailsHBox = new HBox(5, paymentMethodLabel);
                paymentMethodsDetailsHBox.setAlignment(Pos.BASELINE_LEFT);
                paymentMethodsBox = createAndGetDescriptionAndValueBox("bisqEasy.openTrades.tradeDetails.paymentAndSettlementMethods",
                        paymentMethodsDetailsHBox);

                // Deposit transaction ID
                depositTxTitleLabel = getDescriptionLabel("");
                depositTxDetailsLabel = getValueLabel();
                depositTxCopyButton = getCopyButton("");
                HBox depositTxBox = createAndGetDescriptionAndValueBox(depositTxTitleLabel,
                        depositTxDetailsLabel, depositTxCopyButton);

                paymentAccountDataWaitingLabel = getValueLabel();
                takerPaymentAccountDataLabel = getValueLabel();
                paymentAccountDataRefreshButton = new BisqMenuItem("try-again-grey", "try-again-white");
                paymentAccountDataRefreshButton.setTooltip(Res.get("authorizedRole.mediator.mediationCaseDetails.paymentAccountData.fetch"));
                HBox paymentAccountDataDetailsBox = new HBox(8, paymentAccountDataRefreshButton, paymentAccountDataWaitingLabel);
                paymentAccountDataDetailsBox.setAlignment(Pos.BASELINE_LEFT);
                paymentAccountDataBox = createAndGetDescriptionAndValueBox(
                        "authorizedRole.mediator.mediationCaseDetails.paymentAccountData",
                        paymentAccountDataDetailsBox
                );
                takerPaymentAccountDataBox = createAndGetDescriptionAndValueBox(
                        "authorizedRole.mediator.mediationCaseDetails.paymentAccountData.taker",
                        takerPaymentAccountDataLabel
                );
                makerPaymentAccountDataLabel = getValueLabel();
                makerPaymentAccountDataBox = createAndGetDescriptionAndValueBox(
                        "authorizedRole.mediator.mediationCaseDetails.paymentAccountData.maker",
                        makerPaymentAccountDataLabel
                );
                paymentAccountDataWaitingLabel.setText(Res.get("authorizedRole.mediator.mediationCaseDetails.paymentAccountData.waitingForResponse"));
                paymentAccountDataWaitingLabel.getStyleClass().clear();
                paymentAccountDataWaitingLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
                takerPaymentAccountDataLabel.getStyleClass().clear();
                takerPaymentAccountDataLabel.getStyleClass().addAll("text-fill-white", "normal-text");
                makerPaymentAccountDataLabel.getStyleClass().clear();
                makerPaymentAccountDataLabel.getStyleClass().addAll("text-fill-white", "normal-text");

                content = new VBox(10,
                        buyerUserNameBox,
                        sellerUserNameBox,
                        amountAndPriceBox,
                        paymentMethodsBox,
                        paymentAccountDataBox,
                        takerPaymentAccountDataBox,
                        makerPaymentAccountDataBox,
                        depositTxBox);
            } else {
                content = new VBox(10,
                        amountAndPriceBox);
            }

            content.setAlignment(Pos.CENTER_LEFT);
            root.getChildren().add(content);
        }

        @Override
        protected void onViewAttached() {
            if (!model.isCompactView()) {
                buyerUserNameLabel.setText(String.format("%s (%d)", model.getBuyerUserName(), model.getBuyerCaseCountTotal()));
                sellerUserNameLabel.setText(String.format("%s (%d)", model.getSellerUserName(), model.getSellerCaseCountTotal()));
                buyerUserNameLabel.setTooltip(new Tooltip(Res.get(
                        "authorizedRole.mediator.caseCounts.tooltip",
                        model.getBuyerBotId(),
                        model.getBuyerUserId(),
                        model.getBuyerCaseCountOpen(),
                        model.getBuyerCaseCountClosed()
                )));
                sellerUserNameLabel.setTooltip(new Tooltip(Res.get(
                        "authorizedRole.mediator.caseCounts.tooltip",
                        model.getSellerBotId(),
                        model.getSellerUserId(),
                        model.getSellerCaseCountOpen(),
                        model.getSellerCaseCountClosed()
                )));
                buyerUserNameCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBuyerUserName()));
                sellerUserNameCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getSellerUserName()));

                paymentMethodLabel.setText(model.getPaymentMethod());
                paymentMethodsBox.setVisible(model.isPaymentMethodsBoxVisible());
                paymentMethodsBox.setManaged(model.isPaymentMethodsBoxVisible());

                depositTxDetailsLabel.setText(model.getDepositTxId());
                depositTxTitleLabel.setText(Res.get("bisqEasy.openTrades.tradeDetails.txId"));
                depositTxCopyButton.setTooltip(Res.get("bisqEasy.openTrades.tradeDetails.txId.copy"));
                depositTxCopyButton.setVisible(!model.isDepositTxIdEmpty());
                depositTxCopyButton.setManaged(!model.isDepositTxIdEmpty());
                depositTxDetailsLabel.getStyleClass().clear();
                depositTxDetailsLabel.getStyleClass().add(model.isDepositTxIdEmpty()
                        ? "text-fill-grey-dimmed"
                        : "text-fill-white");
                depositTxDetailsLabel.getStyleClass().add("normal-text");
                depositTxCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getDepositTxId()));
                paymentAccountDataBox.visibleProperty().bind(model.getHasPaymentAccountData().not());
                paymentAccountDataBox.managedProperty().bind(model.getHasPaymentAccountData().not());
                paymentAccountDataRefreshButton.visibleProperty().bind(
                        model.getHasPaymentAccountData().not().and(model.getWaitingForPaymentAccountDataResponse().not())
                );
                paymentAccountDataRefreshButton.managedProperty().bind(
                        model.getHasPaymentAccountData().not().and(model.getWaitingForPaymentAccountDataResponse().not())
                );
                paymentAccountDataWaitingLabel.visibleProperty().bind(
                        model.getHasPaymentAccountData().not().and(model.getWaitingForPaymentAccountDataResponse())
                );
                paymentAccountDataWaitingLabel.managedProperty().bind(
                        model.getHasPaymentAccountData().not().and(model.getWaitingForPaymentAccountDataResponse())
                );
                takerPaymentAccountDataBox.visibleProperty().bind(model.getHasPaymentAccountData());
                takerPaymentAccountDataBox.managedProperty().bind(model.getHasPaymentAccountData());
                makerPaymentAccountDataBox.visibleProperty().bind(model.getHasPaymentAccountData());
                makerPaymentAccountDataBox.managedProperty().bind(model.getHasPaymentAccountData());
                takerPaymentAccountDataLabel.textProperty().bind(model.getTakerPaymentAccountData());
                makerPaymentAccountDataLabel.textProperty().bind(model.getMakerPaymentAccountData());
                paymentAccountDataRefreshButton.setOnAction(e -> controller.requestPaymentAccountData());
            }

            nonBtcAmountLabel.setText(model.getNonBtcAmount());
            nonBtcCurrencyLabel.setText(model.getNonBtcCurrency());
            btcAmountLabel.setText(model.getBtcAmount());
            priceLabel.setText(model.getPrice());
            priceCodesLabel.setText(model.getPriceCodes());
            priceSpecLabel.setText(model.getPriceSpec());
        }

        @Override
        protected void onViewDetached() {
            if (!model.isCompactView()) {
                buyerUserNameCopyButton.setOnAction(null);
                sellerUserNameCopyButton.setOnAction(null);
                depositTxCopyButton.setOnAction(null);
                paymentAccountDataRefreshButton.setOnAction(null);
                paymentAccountDataBox.visibleProperty().unbind();
                paymentAccountDataBox.managedProperty().unbind();
                paymentAccountDataRefreshButton.visibleProperty().unbind();
                paymentAccountDataRefreshButton.managedProperty().unbind();
                paymentAccountDataWaitingLabel.visibleProperty().unbind();
                paymentAccountDataWaitingLabel.managedProperty().unbind();
                takerPaymentAccountDataBox.visibleProperty().unbind();
                takerPaymentAccountDataBox.managedProperty().unbind();
                makerPaymentAccountDataBox.visibleProperty().unbind();
                makerPaymentAccountDataBox.managedProperty().unbind();
                takerPaymentAccountDataLabel.textProperty().unbind();
                makerPaymentAccountDataLabel.textProperty().unbind();
            }
        }
    }
}
