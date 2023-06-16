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

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.data.Triple;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.*;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final Label headline, phase1Label, phase2Label, phase3Label, phase4Label, phase5Label;
    private final HBox headerHBox;
    private final Button openTradeGuideButton, collapseButton, expandButton, infoActionButton, openDisputeButton;
    private final Hyperlink openTradeGuide;
    private final List<Triple<HBox, Label, Badge>> phaseItems;
    private final HBox phaseAndInfoHBox;
    private final VBox firstTimeUserVBox;
    private final VBox infoFields;
    private final BisqText infoHeadline;
    private Subscription isCollapsedPin, phaseIndexPin, isBuyerPhase2FieldsVisiblePin;
    private final AutoCompleteComboBox<Account<?, ?>> paymentAccountsComboBox;
    private MaterialTextField btcAddress, txId, buyersBtcBalance;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller) {
        super(new VBox(0), model, controller);

        root.getStyleClass().addAll("bisq-easy-trade-state-bg");
        root.setPadding(new Insets(15, 30, 20, 30));


        // Welcome
        Label welcomeHeadline = new Label(Res.get("bisqEasy.tradeState.welcome.headline"));
        welcomeHeadline.getStyleClass().add("bisq-easy-trade-state-welcome-headline");

        Label welcomeInfo = new Label(Res.get("bisqEasy.tradeState.welcome.info"));
        welcomeInfo.getStyleClass().add("bisq-easy-trade-state-welcome-sub-headline");

        openTradeGuideButton = new Button(Res.get("bisqEasy.tradeState.openTradeGuide"));
        openTradeGuideButton.setDefaultButton(true);

        VBox.setMargin(welcomeHeadline, new Insets(20, 0, 10, 0));
        VBox.setMargin(openTradeGuideButton, new Insets(20, 0, 20, 0));
        firstTimeUserVBox = new VBox(Layout.hLine(), welcomeHeadline, welcomeInfo, openTradeGuideButton);


        // Header
        headline = new Label();
        headline.getStyleClass().add("bisq-easy-trade-state-headline");

        collapseButton = BisqIconButton.createIconButton("collapse");
        expandButton = BisqIconButton.createIconButton("expand");

        HBox.setMargin(collapseButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(expandButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(headline, new Insets(0, 0, 0, -2));
        headerHBox = new HBox(headline, Spacer.fillHBox(), collapseButton, expandButton);
        headerHBox.setCursor(Cursor.HAND);

        Tooltip tooltip = new Tooltip(Res.get("bisqEasy.tradeState.header.expandCollapse.tooltip"));
        tooltip.setStyle("-fx-show-delay: 500ms;");
        Tooltip.install(headerHBox, tooltip);


        // Phase (left side)
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
        VBox phaseVBox = new VBox(
                separator,
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
                openDisputeButton
        );
        phaseVBox.setMinWidth(300);
        phaseVBox.setMaxWidth(phaseVBox.getMinWidth());

        // Info
        infoHeadline = new BisqText();
        infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");
        infoFields = new VBox(10);

        infoActionButton = new Button();
        infoActionButton.setDefaultButton(true);
        // infoActionButton gets added to contextSpecificFields at phase handler
        VBox.setMargin(infoActionButton, new Insets(5, 0, 0, 0));

        VBox.setMargin(infoHeadline, new Insets(12.5, 0, 0, 0));
        VBox.setVgrow(infoFields, Priority.ALWAYS);
        VBox infoVBox = new VBox(10, Layout.hLine(), infoHeadline, infoFields);

        HBox.setHgrow(infoVBox, Priority.ALWAYS);
        HBox.setHgrow(phaseVBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(phaseVBox, infoVBox);

        VBox.setMargin(headerHBox, new Insets(0, 0, 17, 0));
        VBox.setVgrow(firstTimeUserVBox, Priority.ALWAYS);
        root.getChildren().addAll(headerHBox, firstTimeUserVBox, phaseAndInfoHBox);

        //todo
        paymentAccountsComboBox = new AutoCompleteComboBox<>(model.getPaymentAccounts());
        paymentAccountsComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account<?, ? extends PaymentMethod<?>> object) {
                return object != null ? object.getAccountName() : "";
            }

            @Override
            public Account<?, ? extends PaymentMethod<?>> fromString(String string) {
                return null;
            }
        });
    }

    @Override
    protected void onViewAttached() {
        firstTimeUserVBox.visibleProperty().bind(model.getFirstTimeItemsVisible());
        firstTimeUserVBox.managedProperty().bind(model.getFirstTimeItemsVisible());
        phaseAndInfoHBox.visibleProperty().bind(model.getPhaseAndInfoBoxVisible());
        phaseAndInfoHBox.managedProperty().bind(model.getPhaseAndInfoBoxVisible());
        headline.textProperty().bind(model.getTradeInfo());
        infoHeadline.textProperty().bind(model.getPhaseInfo());
        phase1Label.textProperty().bind(model.getPhase1Info());
        phase2Label.textProperty().bind(model.getPhase2Info());
        phase3Label.textProperty().bind(model.getPhase3Info());
        phase4Label.textProperty().bind(model.getPhase4Info());
        phase5Label.textProperty().bind(model.getPhase5Info());
        infoActionButton.textProperty().bind(model.getActionButtonText());
        infoActionButton.visibleProperty().bind(model.getActionButtonVisible());
        infoActionButton.managedProperty().bind(model.getActionButtonVisible());
        openDisputeButton.visibleProperty().bind(model.getOpenDisputeButtonVisible());
        openDisputeButton.managedProperty().bind(model.getOpenDisputeButtonVisible());

        collapseButton.setOnAction(e -> controller.onCollapse());
        expandButton.setOnAction(e -> controller.onExpand());
        headerHBox.setOnMouseClicked(e -> controller.onHeaderClicked());
        infoActionButton.setOnAction(e -> controller.onAction());
        openDisputeButton.setOnAction(e -> controller.onOpenDispute());
        openTradeGuide.setOnAction(e -> controller.onOpenTradeGuide());
        openTradeGuideButton.setOnAction(e -> controller.onOpenTradeGuide());

        isCollapsedPin = EasyBind.subscribe(model.getIsCollapsed(), isCollapsed -> isCollapsedChanged(isCollapsed));
        phaseIndexPin = EasyBind.subscribe(model.getPhaseIndex(), phaseIndex -> phaseIndexChanged(phaseIndex));

        isBuyerPhase2FieldsVisiblePin = EasyBind.subscribe(model.getPhase(), this::applyPhaseChange);

        paymentAccountsComboBox.setOnChangeConfirmed(e -> {
            if (paymentAccountsComboBox.getSelectionModel().getSelectedItem() == null) {
                paymentAccountsComboBox.getSelectionModel().select(model.getSelectedAccount().get());
                return;
            }
            controller.onPaymentAccountSelected(paymentAccountsComboBox.getSelectionModel().getSelectedItem());
        });
    }


    @Override
    protected void onViewDetached() {
        firstTimeUserVBox.visibleProperty().unbind();
        firstTimeUserVBox.managedProperty().unbind();
        phaseAndInfoHBox.visibleProperty().unbind();
        phaseAndInfoHBox.managedProperty().unbind();
        headline.textProperty().unbind();
        infoHeadline.textProperty().unbind();
        phase1Label.textProperty().unbind();
        phase2Label.textProperty().unbind();
        phase3Label.textProperty().unbind();
        phase4Label.textProperty().unbind();
        phase5Label.textProperty().unbind();
        infoActionButton.textProperty().unbind();
        infoActionButton.disableProperty().unbind();
        infoActionButton.visibleProperty().unbind();
        infoActionButton.managedProperty().unbind();
        openDisputeButton.visibleProperty().unbind();
        openDisputeButton.managedProperty().unbind();
        if (buyersBtcBalance != null) {
            buyersBtcBalance.textProperty().unbind();
            buyersBtcBalance = null;
        }
        if (txId != null) {
            txId.textProperty().unbindBidirectional(model.getTxId());
            txId = null;
        }


        collapseButton.setOnAction(null);
        expandButton.setOnAction(null);
        headerHBox.setOnMouseClicked(null);
        infoActionButton.setOnAction(null);
        openDisputeButton.setOnAction(null);
        openTradeGuide.setOnAction(null);
        openTradeGuideButton.setOnAction(null);

        isCollapsedPin.unsubscribe();
        phaseIndexPin.unsubscribe();
        isBuyerPhase2FieldsVisiblePin.unsubscribe();
        paymentAccountsComboBox.setOnChangeConfirmed(null);
    }

    private void isCollapsedChanged(Boolean isCollapsed) {
        collapseButton.setManaged(!isCollapsed);
        collapseButton.setVisible(!isCollapsed);
        expandButton.setManaged(isCollapsed);
        expandButton.setVisible(isCollapsed);

        if (isCollapsed) {
            VBox.setMargin(headerHBox, new Insets(0, 0, -17, 0));
        } else {
            VBox.setMargin(headerHBox, new Insets(0, 0, 17, 0));
        }
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

    private void applyPhaseChange(TradeStateModel.Phase phase) {

        if (phase == null) {
            return;
        }
        infoFields.getChildren().clear();
        if (buyersBtcBalance != null) {
            buyersBtcBalance.textProperty().unbind();
            buyersBtcBalance = null;
        }
        if (txId != null) {
            txId.textProperty().unbindBidirectional(model.getTxId());
            txId = null;
        }
        infoActionButton.disableProperty().unbind();

        switch (phase) {
            case BUYER_PHASE_1:
                infoFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.tradeState.info.buyer.phase1.info"))
                );
                break;
            case BUYER_PHASE_2:
                btcAddress = getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress"), "", true);
                btcAddress.setPromptText(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress.prompt"));
                infoActionButton.disableProperty().bind(btcAddress.textProperty().isEmpty());
                btcAddress.textProperty().bindBidirectional(model.getBuyersBtcAddress());
                MaterialTextArea account = addTextArea(Res.get("bisqEasy.tradeState.info.buyer.phase2.sellersAccount"), model.getSellersPaymentAccountData().get(), false);
                account.textProperty().bind(model.getSellersPaymentAccountData());

                infoFields.getChildren().addAll(
                        getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2.quoteAmount"), model.getFormattedQuoteAmount().get(), false),
                        account,
                        getLabel(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress.headline", model.getFormattedQuoteAmount().get())),
                        btcAddress,
                        getLabel(Res.get("bisqEasy.tradeState.info.buyer.phase2.confirm", model.getFormattedQuoteAmount().get())),
                        infoActionButton
                );
                break;
            case BUYER_PHASE_3:
                infoFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.tradeState.info.buyer.phase3.info", model.getQuoteCode().get()))
                );
                break;
            case BUYER_PHASE_4:
                buyersBtcBalance = getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase4.balance"), model.getBuyersBtcAddress().get(), false);
                buyersBtcBalance.setHelpText(Res.get("bisqEasy.tradeState.info.phase4.balance.help"));
                buyersBtcBalance.textProperty().bind(model.getBuyersBtcBalance());
                infoActionButton.disableProperty().bind(model.getActionButtonDisabled());
                infoFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.tradeState.info.buyer.phase4.info")),
                        buyersBtcBalance,
                        infoActionButton
                );
                break;
            case BUYER_PHASE_5:
                infoFields.getChildren().addAll(
                        getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase5.quoteAmount"), model.getFormattedQuoteAmount().get(), false),
                        getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase5.baseAmount"), model.getFormattedBaseAmount().get(), false),
                        infoActionButton
                );
                break;

            // Seller
            case SELLER_PHASE_1:
                MaterialTextArea accountData = addTextArea(Res.get("bisqEasy.tradeState.info.seller.phase2.accountData"), model.getSellersPaymentAccountData().get(), true);
                accountData.setPromptText(Res.get("bisqEasy.tradeState.info.seller.phase2.accountData.prompt"));
                infoActionButton.disableProperty().bind(accountData.textProperty().isEmpty());
                infoFields.getChildren().addAll(
                        accountData,
                        infoActionButton,
                        Spacer.fillVBox(),
                        getHelpLabel(Res.get("bisqEasy.tradeState.info.seller.phase1.info"))
                );
                break;
            case SELLER_PHASE_2:
                infoFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.tradeState.info.seller.phase2.info", model.getQuoteCode().get()))
                );
                break;
            case SELLER_PHASE_3:
                txId = getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.txId"), "", true);
                txId.textProperty().bindBidirectional(model.getTxId());
                txId.setPromptText(Res.get("bisqEasy.tradeState.info.seller.phase3.txId.prompt"));
                infoActionButton.disableProperty().bind(txId.textProperty().isEmpty());
                infoFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.tradeState.info.seller.phase3.sendBtc", model.getQuoteCode().get(), model.getFormattedBaseAmount().get())),
                        getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.baseAmount"), model.getFormattedBaseAmount().get(), false),
                        getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.btcAddress"), model.getBuyersBtcAddress().get(), false),
                        txId,
                        infoActionButton
                );
                break;
            case SELLER_PHASE_4:
                buyersBtcBalance = getTextField(Res.get("bisqEasy.tradeState.info.seller.phase4.balance"), model.getBuyersBtcAddress().get(), false);
                buyersBtcBalance.textProperty().bind(model.getBuyersBtcBalance());
                buyersBtcBalance.setHelpText(Res.get("bisqEasy.tradeState.info.phase4.balance.help"));
                infoActionButton.disableProperty().bind(model.getActionButtonDisabled());
                infoFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.tradeState.info.seller.phase4.info")),
                        buyersBtcBalance,
                        infoActionButton
                );
                break;
            case SELLER_PHASE_5:
                infoFields.getChildren().addAll(
                        getTextField(Res.get("bisqEasy.tradeState.info.seller.phase5.quoteAmount"), model.getFormattedQuoteAmount().get(), false),
                        getTextField(Res.get("bisqEasy.tradeState.info.seller.phase5.baseAmount"), model.getFormattedBaseAmount().get(), false),
                        infoActionButton
                );
                break;
        }
    }

    // Utils
    private static Label getLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("bisq-easy-trade-state-info-text");
        label.setWrapText(true);
        VBox.setMargin(label, new Insets(10, 0, 0, 0));
        return label;
    }

    private static Label getHelpLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("bisq-easy-trade-state-info-help-text");
        label.setWrapText(true);
        VBox.setMargin(label, new Insets(10, 0, 0, 0));
        return label;
    }

    private static MaterialTextField getTextField(String description, String value, boolean isEditable) {
        MaterialTextField field = new MaterialTextField(description, null);
        field.setText(value);
        field.showCopyIcon();
        field.setEditable(isEditable);
        VBox.setMargin(field, new Insets(0, 0, 0, 0));
        if (isEditable) {
            UIThread.runOnNextRenderFrame(field::requestFocus);
        }
        return field;
    }

    private static MaterialTextArea addTextArea(String description, String value, boolean isEditable) {
        MaterialTextArea field = new MaterialTextArea(description, null);
        field.setText(value);
        field.showCopyIcon();
        field.setEditable(isEditable);
        field.setFixedHeight(107);
        VBox.setMargin(field, new Insets(0, 0, 0, 0));
        if (isEditable) {
            UIThread.runOnNextRenderFrame(field::requestFocus);
        }
        return field;
    }

    private static Region getVLine() {
        Region separator = Layout.vLine();
        separator.setMinHeight(10);
        separator.setMaxHeight(separator.getMinHeight());
        VBox.setMargin(separator, new Insets(5, 0, 5, 17));
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
