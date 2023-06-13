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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_state;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.data.Triple;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
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
    private final Label tradeInfo, phaseInfo, phase2Label, phase3Label;
    //   private final VBox contentPane;
    private final HBox headerHBox;
    private final Button collapseButton, expandButton, actionButton, openDisputeButton;
    private final Hyperlink openTradeGuide;
    private final List<Triple<HBox, Label, Badge>> phaseItems;
    private final HBox phaseAndInfoBox;
    private Subscription isCollapsedPin, activePhaseIndexPin;
    private final AutoCompleteComboBox<Account<?, ?>> paymentAccountsComboBox;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller) {
        super(new VBox(0), model, controller);

        root.getStyleClass().addAll("bisq-box-2");
        root.setPadding(new Insets(15, 30, 30, 30));

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

        Tooltip tooltip = new Tooltip(Res.get("bisqEasy.assistant.header.expandCollapse.tooltip"));
        tooltip.setStyle("-fx-show-delay: 500ms;");
        Tooltip.install(headerHBox, tooltip);

        // Phase progress
        Label phaseHeadline = new Label(Res.get("bisqEasy.assistant.tradeState.phase.headline"));
        phaseHeadline.getStyleClass().add("bisq-easy-trade-state-sub-headline");

        Triple<HBox, Label, Badge> phaseItem1 = getPhaseItem(1, Res.get("bisqEasy.assistant.tradeState.phase.phase1"));
        Triple<HBox, Label, Badge> phaseItem2 = getPhaseItem(2);
        Triple<HBox, Label, Badge> phaseItem3 = getPhaseItem(3);
        Triple<HBox, Label, Badge> phaseItem4 = getPhaseItem(4, Res.get("bisqEasy.assistant.tradeState.phase.phase4"));

        HBox phase1HBox = phaseItem1.getFirst();
        HBox phase2HBox = phaseItem2.getFirst();
        HBox phase3HBox = phaseItem3.getFirst();
        HBox phase4HBox = phaseItem4.getFirst();

        phase2Label = phaseItem2.getSecond();
        phase3Label = phaseItem3.getSecond();
        phaseItems = List.of(phaseItem1, phaseItem2, phaseItem3, phaseItem4);

        openTradeGuide = new Hyperlink(Res.get("bisqEasy.assistant.header.openTradeGuide"));

        openDisputeButton = new Button(Res.get("bisqEasy.openDispute"));
        // openDisputeButton.getStyleClass().add("grey-transparent-outlined-button");
        openDisputeButton.getStyleClass().add("outlined-button");

        Region separator = Layout.separator();
        Region separator2 = Layout.separator();

        // VBox.setMargin(separator, new Insets(0, 0, 20, 0));
        VBox.setMargin(phaseHeadline, new Insets(20, 0, 20, 0));
        VBox.setMargin(openTradeGuide, new Insets(30, 0, 0, 2));
        VBox.setMargin(separator2, new Insets(10, 0, 20, 0));
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
                Spacer.fillVBox(),
                openTradeGuide,
                separator2,
                openDisputeButton
        );
        phaseBox.setMinWidth(300);
        phaseBox.setMaxWidth(phaseBox.getMinWidth());

        // Phase info
        Label phaseInfoHeadline = new Label(Res.get("bisqEasy.assistant.tradeState.phaseInfo.headline"));
        phaseInfoHeadline.getStyleClass().add("bisq-easy-trade-state-sub-headline");

        phaseInfo = new Label();
        phaseInfo.getStyleClass().add("bisq-easy-trade-state-info-text");
        phaseInfo.setWrapText(true);

        actionButton = new Button();
        actionButton.setDefaultButton(true);

        VBox infoBox = new VBox(20, Layout.separator(), phaseInfoHeadline, phaseInfo, Spacer.fillVBox(), Layout.separator(), actionButton);

        HBox.setHgrow(infoBox, Priority.ALWAYS);
        phaseAndInfoBox = new HBox(phaseBox, infoBox);


        VBox.setMargin(headerHBox, new Insets(0, 0, 17, 0));
        root.getChildren().addAll(headerHBox, phaseAndInfoBox);


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
        tradeInfo.textProperty().bind(model.getTradeInfo());
        collapseButton.setOnAction(e -> controller.onCollapse());
        expandButton.setOnAction(e -> controller.onExpand());
        headerHBox.setOnMouseClicked(e -> controller.onHeaderClicked());
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

            phaseAndInfoBox.setManaged(!isCollapsed);
            phaseAndInfoBox.setVisible(!isCollapsed);
        });


        phaseInfo.textProperty().bind(model.getPhaseInfo());
        phase2Label.textProperty().bind(model.getPhase2());
        phase3Label.textProperty().bind(model.getPhase3());
        actionButton.textProperty().bind(model.getActionButtonText());
        actionButton.visibleProperty().bind(model.getActionButtonVisible());
        actionButton.managedProperty().bind(model.getActionButtonVisible());
        openDisputeButton.visibleProperty().bind(model.getOpenDisputeButtonVisible());
        openDisputeButton.managedProperty().bind(model.getOpenDisputeButtonVisible());

        actionButton.setOnAction(e -> controller.onAction());
        openDisputeButton.setOnAction(e -> controller.onOpenDispute());
        openTradeGuide.setOnAction(e -> controller.onOpenTradeGuide());

        activePhaseIndexPin = EasyBind.subscribe(model.getActivePhaseIndex(),
                activePhaseIndex -> {
                    for (int i = 0; i < phaseItems.size(); i++) {
                        Badge badge = phaseItems.get(i).getThird();
                        Label label = phaseItems.get(i).getSecond();
                        if (activePhaseIndex.intValue() == i) {
                            //  badge.setText("✔"); //✓ ✔
                            badge.getStyleClass().remove("bisq-easy-trade-state-badge-inactive");
                            badge.getStyleClass().add("bisq-easy-trade-state-badge-active");
                            label.getStyleClass().remove("bisq-easy-trade-state-inactive");
                            label.getStyleClass().add("bisq-easy-trade-state-active");
                        } else {
                            badge.getStyleClass().remove("bisq-easy-trade-state-badge-active");
                            badge.getStyleClass().add("bisq-easy-trade-state-badge-inactive");
                            label.getStyleClass().remove("bisq-easy-trade-state-active");
                            label.getStyleClass().add("bisq-easy-trade-state-inactive");
                        }
                    }
                });


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
        tradeInfo.textProperty().unbind();
        collapseButton.setOnAction(null);
        expandButton.setOnAction(null);
        headerHBox.setOnMouseClicked(null);
        isCollapsedPin.unsubscribe();


        phaseInfo.textProperty().unbind();
        phase2Label.textProperty().unbind();
        phase3Label.textProperty().unbind();
        actionButton.textProperty().unbind();
        actionButton.visibleProperty().unbind();
        actionButton.managedProperty().unbind();
        openDisputeButton.visibleProperty().unbind();
        openDisputeButton.managedProperty().unbind();

        actionButton.setOnAction(null);
        openDisputeButton.setOnAction(null);
        openTradeGuide.setOnAction(null);

        activePhaseIndexPin.unsubscribe();
        paymentAccountsComboBox.setOnChangeConfirmed(null);
    }

    private Separator getVLine() {
        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setMinHeight(30);
        separator.setPadding(new Insets(7.5, 0, 7.5, 17));
        return separator;
    }

    private Triple<HBox, Label, Badge> getPhaseItem(int index) {
        return getPhaseItem(index, null);
    }

    private Triple<HBox, Label, Badge> getPhaseItem(int index, @Nullable String text) {
        Label label = text != null ? new Label(text.toUpperCase()) : new Label();
        label.getStyleClass().add("bisq-easy-trade-state");
        Badge badge = new Badge();
        badge.getStyleClass().clear();
        badge.getStyleClass().add("bisq-easy-trade-state-badge");
        badge.setText(String.valueOf(index));
        badge.setPrefSize(30, 30);
        HBox hBox = new HBox(7.5, badge, label);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return new Triple<>(hBox, label, badge);
    }

    private void applyProgress(int progressIndex, boolean delay) {
        if (progressIndex < phaseItems.size()) {
           /* phaseItems.forEach(label -> label.setOpacity(OPACITY));
            Label label = phaseItems.get(progressIndex);
            if (delay) {
                UIScheduler.run(() -> Transitions.fade(label, OPACITY, 1, Transitions.DEFAULT_DURATION / 2))
                        .after(Transitions.DEFAULT_DURATION / 2);
            } else {
                label.setOpacity(1);
            }*/
        }
    }
}
