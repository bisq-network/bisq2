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
import bisq.common.observable.Pin;
import bisq.contract.Role;
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
import bisq.support.mediation.MediationCaseState;
import bisq.support.mediation.mu_sig.MuSigMediationCase;
import bisq.support.mediation.mu_sig.MuSigMediationIssue;
import bisq.support.mediation.mu_sig.MuSigMediationIssueType;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        private final Set<Pin> pins = new HashSet<>();

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
            model.getTakerPaymentAccountIssueVisible().set(false);
            model.getMakerPaymentAccountIssueVisible().set(false);
            model.getTakerPaymentAccountIssueTooltip().set("");
            model.getMakerPaymentAccountIssueTooltip().set("");
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
            boolean requestSent = muSigMediatorService.requestPaymentDetails(muSigMediationCaseListItem.getMuSigMediationCase());
            model.getWaitingForPaymentAccountDataResponse().set(requestSent);
        }

        @Override
        public void onDeactivate() {
            clearPins();
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
            clearPins();

            MuSigMediationCaseListItem muSigMediationCaseListItem = model.getMuSigMediationCaseListItem();
            if (muSigMediationCaseListItem == null) {
                return;
            }

            MuSigMediationCase caseModel = muSigMediationCaseListItem.getMuSigMediationCase();
            pins.add(caseModel.getTakerAccountPayload().addObserver(payload -> UIThread.run(() -> {
                model.getTakerPaymentAccountData().set(payload.map(AccountPayload::getAccountDataDisplayString).orElse(""));
                applyHasPaymentAccountDataState(caseModel);
            })));
            pins.add(caseModel.getMakerAccountPayload().addObserver(payload -> UIThread.run(() -> {
                model.getMakerPaymentAccountData().set(payload.map(AccountPayload::getAccountDataDisplayString).orElse(""));
                applyHasPaymentAccountDataState(caseModel);
            })));
            pins.add(caseModel.getIssues().addObserver(issues ->
                    UIThread.run(() -> applyPaymentAccountIssueState(issues))));
        }

        private void clearPins() {
            pins.forEach(Pin::unbind);
            pins.clear();
        }

        private void applyHasPaymentAccountDataState(MuSigMediationCase muSigMediationCase) {
            boolean hasBothPayloads = muSigMediationCase.getTakerAccountPayload().get().isPresent()
                    && muSigMediationCase.getMakerAccountPayload().get().isPresent();
            model.getHasPaymentAccountData().set(hasBothPayloads);
            if (hasBothPayloads) {
                model.getWaitingForPaymentAccountDataResponse().set(false);
            }
        }

        private void applyPaymentAccountIssueState(List<MuSigMediationIssue> issues) {
            List<MuSigMediationIssue> takerIssues = findFirstIssuesByTypeAndRole(issues, MuSigMediationIssueType.TAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH);
            List<MuSigMediationIssue> makerIssues = findFirstIssuesByTypeAndRole(issues, MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH);
            boolean hasIssue = !takerIssues.isEmpty() || !makerIssues.isEmpty();

            model.getTakerPaymentAccountIssueVisible().set(!takerIssues.isEmpty());
            model.getMakerPaymentAccountIssueVisible().set(!makerIssues.isEmpty());
            model.getTakerPaymentAccountIssueTooltip().set(takerIssues.isEmpty() ? "" : toIssueTooltip(takerIssues));
            model.getMakerPaymentAccountIssueTooltip().set(makerIssues.isEmpty() ? "" : toIssueTooltip(makerIssues));
            if (hasIssue) {
                model.getWaitingForPaymentAccountDataResponse().set(false);
            }
            if (!takerIssues.isEmpty() && model.getTakerPaymentAccountData().get().isEmpty()) {
                model.getTakerPaymentAccountData().set(Res.get("data.na"));
            }
            if (!makerIssues.isEmpty() && model.getMakerPaymentAccountData().get().isEmpty()) {
                model.getMakerPaymentAccountData().set(Res.get("data.na"));
            }
        }

        private List<MuSigMediationIssue> findFirstIssuesByTypeAndRole(List<MuSigMediationIssue> issues,
                                                                       MuSigMediationIssueType type) {
            return Stream.of(Role.TAKER, Role.MAKER)
                    .flatMap(role -> issues.stream()
                            .filter(issue -> issue.getType() == type && issue.getCausingRole() == role)
                            .findFirst()
                            .stream())
                    .toList();
        }

        private String toIssueTooltip(List<MuSigMediationIssue> issues) {
            String key = "authorizedRole.mediator.mediationCaseDetails.paymentAccountData.issue.accountPayloadHashMismatch";
            List<String> lines = new ArrayList<>();
            for (MuSigMediationIssue issue : issues) {
                String causingRole = MuSigTradeFormatter.getMakerTakerRole(issue.getCausingRole() == Role.MAKER).toLowerCase();
                StringBuilder builder = new StringBuilder(Res.get(key, causingRole));
                issue.getDetails().ifPresent(details -> builder.append("\n").append(details));
                lines.add(builder.toString());
            }
            return String.join("\n\n", lines);
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
        private final BooleanProperty takerPaymentAccountIssueVisible = new SimpleBooleanProperty(false);
        private final BooleanProperty makerPaymentAccountIssueVisible = new SimpleBooleanProperty(false);
        private final StringProperty takerPaymentAccountIssueTooltip = new SimpleStringProperty("");
        private final StringProperty makerPaymentAccountIssueTooltip = new SimpleStringProperty("");

        private String depositTxId;
        private boolean isDepositTxIdEmpty;
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private final Label nonBtcAmountLabel, nonBtcCurrencyLabel, btcAmountLabel, priceLabel,
                priceCodesLabel, priceSpecLabel;
        private Label buyerUserNameLabel, sellerUserNameLabel, paymentMethodLabel, depositTxTitleLabel, depositTxDetailsLabel;
        private Label paymentAccountDataWaitingLabel, takerPaymentAccountDataLabel, makerPaymentAccountDataLabel;
        private Button takerPaymentAccountIssueButton, makerPaymentAccountIssueButton;
        private Tooltip takerPaymentAccountIssueTooltip, makerPaymentAccountIssueTooltip;
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
                takerPaymentAccountIssueTooltip = new Tooltip();
                makerPaymentAccountIssueTooltip = new Tooltip();
                HBox paymentAccountDataDetailsBox = new HBox(8, paymentAccountDataRefreshButton, paymentAccountDataWaitingLabel);
                paymentAccountDataDetailsBox.setAlignment(Pos.BASELINE_LEFT);
                paymentAccountDataBox = createAndGetDescriptionAndValueBox(
                        "authorizedRole.mediator.mediationCaseDetails.paymentAccountData",
                        paymentAccountDataDetailsBox
                );
                takerPaymentAccountDataBox = createAndGetDescriptionAndValueBox(
                        "authorizedRole.mediator.mediationCaseDetails.paymentAccountData.taker",
                        createPaymentAccountDataWithIssueBox(true)
                );
                makerPaymentAccountDataLabel = getValueLabel();
                makerPaymentAccountDataBox = createAndGetDescriptionAndValueBox(
                        "authorizedRole.mediator.mediationCaseDetails.paymentAccountData.maker",
                        createPaymentAccountDataWithIssueBox(false)
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
                paymentAccountDataBox.visibleProperty().bind(
                        model.getHasPaymentAccountData().not()
                                .and(model.getTakerPaymentAccountIssueVisible().not())
                                .and(model.getMakerPaymentAccountIssueVisible().not())
                );
                paymentAccountDataBox.managedProperty().bind(
                        model.getHasPaymentAccountData().not()
                                .and(model.getTakerPaymentAccountIssueVisible().not())
                                .and(model.getMakerPaymentAccountIssueVisible().not())
                );
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
                takerPaymentAccountDataBox.visibleProperty().bind(
                        model.getTakerPaymentAccountData().isNotEmpty()
                                .or(model.getTakerPaymentAccountIssueVisible())
                );
                takerPaymentAccountDataBox.managedProperty().bind(
                        model.getTakerPaymentAccountData().isNotEmpty()
                                .or(model.getTakerPaymentAccountIssueVisible())
                );
                makerPaymentAccountDataBox.visibleProperty().bind(
                        model.getMakerPaymentAccountData().isNotEmpty()
                                .or(model.getMakerPaymentAccountIssueVisible())
                );
                makerPaymentAccountDataBox.managedProperty().bind(
                        model.getMakerPaymentAccountData().isNotEmpty()
                                .or(model.getMakerPaymentAccountIssueVisible())
                );
                takerPaymentAccountDataLabel.textProperty().bind(model.getTakerPaymentAccountData());
                makerPaymentAccountDataLabel.textProperty().bind(model.getMakerPaymentAccountData());
                takerPaymentAccountIssueButton.visibleProperty().bind(model.getTakerPaymentAccountIssueVisible());
                takerPaymentAccountIssueButton.managedProperty().bind(model.getTakerPaymentAccountIssueVisible());
                makerPaymentAccountIssueButton.visibleProperty().bind(model.getMakerPaymentAccountIssueVisible());
                makerPaymentAccountIssueButton.managedProperty().bind(model.getMakerPaymentAccountIssueVisible());
                takerPaymentAccountIssueTooltip.textProperty().bind(model.getTakerPaymentAccountIssueTooltip());
                makerPaymentAccountIssueTooltip.textProperty().bind(model.getMakerPaymentAccountIssueTooltip());
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
                takerPaymentAccountIssueButton.visibleProperty().unbind();
                takerPaymentAccountIssueButton.managedProperty().unbind();
                makerPaymentAccountIssueButton.visibleProperty().unbind();
                makerPaymentAccountIssueButton.managedProperty().unbind();
                takerPaymentAccountIssueTooltip.textProperty().unbind();
                makerPaymentAccountIssueTooltip.textProperty().unbind();
            }
        }

        private HBox createPaymentAccountDataWithIssueBox(boolean taker) {
            Button issueButton = bisq.desktop.components.controls.BisqIconButton.createInfoIconButton();
            issueButton.getGraphic().getStyleClass().add("overlay-icon-warning");
            issueButton.setVisible(false);
            issueButton.setManaged(false);
            if (taker) {
                takerPaymentAccountIssueButton = issueButton;
                issueButton.setTooltip(takerPaymentAccountIssueTooltip);
            } else {
                makerPaymentAccountIssueButton = issueButton;
                issueButton.setTooltip(makerPaymentAccountIssueTooltip);
            }
            Label valueLabel = taker ? takerPaymentAccountDataLabel : makerPaymentAccountDataLabel;
            HBox box = new HBox(6, valueLabel, issueButton);
            box.setAlignment(Pos.BASELINE_LEFT);
            return box;
        }
    }
}
