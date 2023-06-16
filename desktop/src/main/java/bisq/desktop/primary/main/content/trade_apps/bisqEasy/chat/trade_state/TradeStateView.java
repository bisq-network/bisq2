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

import javax.annotation.Nullable;
import java.util.List;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final Label tradeInfo, phase1Label, phase2Label, phase3Label, phase4Label, phase5Label;
    private final HBox headerHBox;
    private final Button openTradeGuideButton, collapseButton, expandButton, actionButton, openDisputeButton;
    private final Hyperlink openTradeGuide;
    private final List<Triple<HBox, Label, Badge>> phaseItems;
    private final HBox phaseAndInfoBox;
    private final VBox firstTimeUserBox;
    private final VBox contextSpecificFields;
    private final BisqText instructionHeadline;
    private Subscription isCollapsedPin, phaseIndexPin, isBuyerPhase2FieldsVisiblePin;
    private final AutoCompleteComboBox<Account<?, ?>> paymentAccountsComboBox;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller) {
        super(new VBox(0), model, controller);

        root.getStyleClass().addAll("bisq-box-2");
        root.setPadding(new Insets(15, 30, 30, 30));

        // Welcome for first time trader
        Label firstTimeHeadline = new Label(Res.get("bisqEasy.trade.state.phaseInfo.phase1.firstTime.headline"));
        firstTimeHeadline.getStyleClass().add("bisq-easy-trade-state-welcome-headline");

        Label firstTimeInfo = new Label(Res.get("bisqEasy.trade.state.phaseInfo.phase1.firstTime.info"));
        firstTimeInfo.getStyleClass().add("bisq-easy-trade-state-welcome-sub-headline");

        openTradeGuideButton = new Button(Res.get("bisqEasy.trade.state.phaseInfo.phase1.firstTime.openTradeGuide"));
        openTradeGuideButton.setDefaultButton(true);

        // Header
        tradeInfo = new Label();
        tradeInfo.getStyleClass().add("bisq-easy-trade-state-headline");

        collapseButton = BisqIconButton.createIconButton("collapse");
        expandButton = BisqIconButton.createIconButton("expand");

        HBox.setMargin(collapseButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(expandButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(tradeInfo, new Insets(0, 0, 0, -2));
        headerHBox = new HBox(tradeInfo, Spacer.fillHBox(), collapseButton, expandButton);
        headerHBox.setCursor(Cursor.HAND);

        Tooltip tooltip = new Tooltip(Res.get("bisqEasy.trade.header.expandCollapse.tooltip"));
        tooltip.setStyle("-fx-show-delay: 500ms;");
        Tooltip.install(headerHBox, tooltip);

        // Phase progress
        Label phaseHeadline = new Label(Res.get("bisqEasy.trade.state.phase.headline"));
        phaseHeadline.getStyleClass().add("bisq-easy-trade-state-sub-headline");

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

        openTradeGuide = new Hyperlink(Res.get("bisqEasy.trade.header.openTradeGuide"));

        openDisputeButton = new Button(Res.get("bisqEasy.openDispute"));
        openDisputeButton.getStyleClass().add("outlined-button");

        Region separator = Layout.hLine();

        VBox.setMargin(phaseHeadline, new Insets(20, 0, 20, 0));
        VBox.setMargin(openTradeGuide, new Insets(30, 0, 10, 2));
        VBox phaseBox = new VBox(
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
        phaseBox.setMinWidth(300);
        phaseBox.setMaxWidth(phaseBox.getMinWidth());


        actionButton = new Button();
        actionButton.setDefaultButton(true);

        VBox.setMargin(actionButton, new Insets(5, 0, 0, 0));

        VBox.setMargin(firstTimeHeadline, new Insets(20, 0, 10, 0));
        VBox.setMargin(openTradeGuideButton, new Insets(20, 0, 20, 0));
        firstTimeUserBox = new VBox(Layout.hLine(), firstTimeHeadline, firstTimeInfo, openTradeGuideButton);

        instructionHeadline = new BisqText();
        instructionHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");
        contextSpecificFields = new VBox(10);

        VBox.setMargin(instructionHeadline, new Insets(12.5, 0, 0, 0));
        VBox instructionsBox = new VBox(10, Layout.hLine(), instructionHeadline, contextSpecificFields);

        HBox.setHgrow(instructionsBox, Priority.ALWAYS);
        HBox.setHgrow(firstTimeUserBox, Priority.ALWAYS);
        phaseAndInfoBox = new HBox(phaseBox, instructionsBox);

        VBox.setMargin(headerHBox, new Insets(0, 0, 17, 0));
        root.getChildren().addAll(headerHBox, firstTimeUserBox, phaseAndInfoBox);

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
        firstTimeUserBox.visibleProperty().bind(model.getFirstTimeItemsVisible());
        firstTimeUserBox.managedProperty().bind(model.getFirstTimeItemsVisible());
        phaseAndInfoBox.visibleProperty().bind(model.getPhaseAndInfoBoxVisible());
        phaseAndInfoBox.managedProperty().bind(model.getPhaseAndInfoBoxVisible());
        tradeInfo.textProperty().bind(model.getTradeInfo());
        instructionHeadline.textProperty().bind(model.getPhaseInfo());
        phase1Label.textProperty().bind(model.getPhase1Info());
        phase2Label.textProperty().bind(model.getPhase2Info());
        phase3Label.textProperty().bind(model.getPhase3Info());
        phase4Label.textProperty().bind(model.getPhase4Info());
        phase5Label.textProperty().bind(model.getPhase5Info());
        actionButton.textProperty().bind(model.getActionButtonText());
        actionButton.visibleProperty().bind(model.getActionButtonVisible());
        actionButton.managedProperty().bind(model.getActionButtonVisible());
        openDisputeButton.visibleProperty().bind(model.getOpenDisputeButtonVisible());
        openDisputeButton.managedProperty().bind(model.getOpenDisputeButtonVisible());

        collapseButton.setOnAction(e -> controller.onCollapse());
        expandButton.setOnAction(e -> controller.onExpand());
        headerHBox.setOnMouseClicked(e -> controller.onHeaderClicked());
        actionButton.setOnAction(e -> controller.onAction());
        openDisputeButton.setOnAction(e -> controller.onOpenDispute());
        openTradeGuide.setOnAction(e -> controller.onOpenTradeGuide());
        openTradeGuideButton.setOnAction(e -> controller.onOpenTradeGuide());

        isCollapsedPin = EasyBind.subscribe(model.getIsCollapsed(), isCollapsed -> {
            collapseButton.setManaged(!isCollapsed);
            collapseButton.setVisible(!isCollapsed);
            expandButton.setManaged(isCollapsed);
            expandButton.setVisible(isCollapsed);

            if (isCollapsed) {
                VBox.setMargin(headerHBox, new Insets(0, 0, -17, 0));
            } else {
                VBox.setMargin(headerHBox, new Insets(0, 0, 17, 0));
            }
        });

        phaseIndexPin = EasyBind.subscribe(model.getPhaseIndex(),
                phaseIndex -> {
                    for (int i = 0; i < phaseItems.size(); i++) {
                        Badge badge = phaseItems.get(i).getThird();
                        Label label = phaseItems.get(i).getSecond();
                        badge.getStyleClass().clear();
                        label.getStyleClass().clear();
                        badge.getStyleClass().add("bisq-easy-trade-state-badge");
                        if (i <= phaseIndex.intValue()) {
                            badge.getStyleClass().add("bisq-easy-trade-state-badge-active");
                            label.getStyleClass().add("bisq-easy-trade-state-active");
                        } else {
                            badge.getStyleClass().add("bisq-easy-trade-state-badge-inactive");
                            label.getStyleClass().add("bisq-easy-trade-state-inactive");
                        }
                    }
                });

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
        firstTimeUserBox.visibleProperty().unbind();
        firstTimeUserBox.managedProperty().unbind();
        phaseAndInfoBox.visibleProperty().unbind();
        phaseAndInfoBox.managedProperty().unbind();
        tradeInfo.textProperty().unbind();
        instructionHeadline.textProperty().unbind();
        phase1Label.textProperty().unbind();
        phase2Label.textProperty().unbind();
        phase3Label.textProperty().unbind();
        phase4Label.textProperty().unbind();
        phase5Label.textProperty().unbind();
        actionButton.textProperty().unbind();
        actionButton.visibleProperty().unbind();
        actionButton.managedProperty().unbind();
        openDisputeButton.visibleProperty().unbind();
        openDisputeButton.managedProperty().unbind();

        collapseButton.setOnAction(null);
        expandButton.setOnAction(null);
        headerHBox.setOnMouseClicked(null);
        actionButton.setOnAction(null);
        openDisputeButton.setOnAction(null);
        openTradeGuide.setOnAction(null);
        openTradeGuideButton.setOnAction(null);

        isCollapsedPin.unsubscribe();
        phaseIndexPin.unsubscribe();
        isBuyerPhase2FieldsVisiblePin.unsubscribe();
        paymentAccountsComboBox.setOnChangeConfirmed(null);
    }

    private void applyPhaseChange(TradeStateModel.Phase phase) {
        contextSpecificFields.getChildren().clear();
        if (phase == null) {
            return;
        }
        switch (phase) {
            case BUYER_PHASE_1:
                contextSpecificFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase1.info"))
                );
                break;
            case BUYER_PHASE_2:
                MaterialTextField btcAddress = getTextField(Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase2.btcAddress"), model.getBuyersBtcAddress().get(), true);
                btcAddress.setPromptText(Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase2.btcAddress.prompt"));
                MaterialTextArea account = addTextArea(Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase2.sellersAccount"), model.getSellersPaymentAccountData().get(), false);
                account.setText("IBAN: 123123123\nBIC:123123:\nName: Ben Toshi");
                contextSpecificFields.getChildren().addAll(
                        getTextField(Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase2.quoteAmount"), model.getQuoteAmount().get(), false),
                        account,
                        getLabel(Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase2.btcAddress.headline", model.getQuoteAmount().get())),
                        btcAddress,
                        getLabel(Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase2.confirm", model.getQuoteAmount().get()))
                );
                contextSpecificFields.getChildren().add(actionButton);
                break;
            case BUYER_PHASE_3:
                contextSpecificFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase3.info"))
                );
                break;
            case BUYER_PHASE_4:
                contextSpecificFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase4.info"))
                );
                contextSpecificFields.getChildren().add(actionButton);
                break;
            case BUYER_PHASE_5:
                contextSpecificFields.getChildren().addAll(
                        getTextField(Res.get("bisqEasy.trade.state.phase.buyer.phase5.quoteAmount"), model.getQuoteAmount().get(), false),
                        getTextField(Res.get("bisqEasy.trade.state.phase.buyer.phase5.baseAmount"), model.getBaseAmount().get(), false)
                );
                contextSpecificFields.getChildren().add(actionButton);
                break;

            // Seller
            case SELLER_PHASE_1:
                MaterialTextArea accountData = addTextArea(Res.get("bisqEasy.trade.state.phase.seller.phase2.accountData"), model.getSellersPaymentAccountData().get(), true);
                accountData.setPromptText("Fill in your payment account data. E.g. IBAN, BIC and account owner name");
                contextSpecificFields.getChildren().addAll(
                        accountData
                );
                contextSpecificFields.getChildren().add(actionButton);
                break;
            case SELLER_PHASE_2:
                contextSpecificFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.trade.state.phaseInfo.seller.phase2.info"))
                );
                break;
            case SELLER_PHASE_3:
                contextSpecificFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.trade.state.phaseInfo.seller.phase3.sendBtc", model.getQuoteCode().get(), model.getBaseAmount().get())),
                        getTextField(Res.get("bisqEasy.trade.state.phaseInfo.seller.phase3.baseAmount"), model.getBaseAmount().get(), false),
                        getTextField(Res.get("bisqEasy.trade.state.phaseInfo.seller.phase3.btcAddress"), model.getBuyersBtcAddress().get(), false)
                );
                contextSpecificFields.getChildren().add(actionButton);
                break;
            case SELLER_PHASE_4:
                contextSpecificFields.getChildren().addAll(
                        getLabel(Res.get("bisqEasy.trade.state.phaseInfo.seller.phase4.info"))
                );
                break;
            case SELLER_PHASE_5:
                contextSpecificFields.getChildren().addAll(
                        getTextField(Res.get("bisqEasy.trade.state.phase.seller.phase5.quoteAmount"), model.getQuoteAmount().get(), false),
                        getTextField(Res.get("bisqEasy.trade.state.phase.seller.phase5.baseAmount"), model.getBaseAmount().get(), false)
                );
                contextSpecificFields.getChildren().add(actionButton);
                break;
        }
    }

    private Label getLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("bisq-easy-trade-state-info-text");
        label.setWrapText(true);
        VBox.setMargin(label, new Insets(10, 0, 0, 0));
        return label;
    }

    private MaterialTextField getTextField(String description, String value, boolean isEditable) {
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

    private MaterialTextArea addTextArea(String description, String value, boolean isEditable) {
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

    private Region getVLine() {
        Region separator = Layout.vLine();
        separator.setMinHeight(10);
        separator.setMaxHeight(separator.getMinHeight());
        VBox.setMargin(separator, new Insets(5, 0, 5, 17));
        return separator;
    }

    private Triple<HBox, Label, Badge> getPhaseItem(int index) {
        return getPhaseItem(index, null);
    }

    private Triple<HBox, Label, Badge> getPhaseItem(int index, @Nullable String text) {
        Label label = text != null ? new Label(text.toUpperCase()) : new Label();
        label.getStyleClass().add("bisq-easy-trade-state");
        Badge badge = new Badge();
        badge.setText(String.valueOf(index));
        badge.setPrefSize(20, 20);
        HBox hBox = new HBox(7.5, badge, label);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return new Triple<>(hBox, label, badge);
    }
}
