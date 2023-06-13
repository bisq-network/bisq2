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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.state;

import bisq.common.data.Triple;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.List;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final List<Triple<HBox, Label, Badge>> phaseItems;
    private final Button nextButton, actionButton, openDisputeButton;
    private final Hyperlink openTradeGuide;
    private final Label phaseInfo, phase2Label, phase3Label;
    private final Label tradeInfo;
    private Subscription activePhaseIndexPin, widthPin;
    private Subscription topPaneBoxVisibleSubscription;
    private final ChangeListener<Number> currentIndexListener;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        tradeInfo = new Label();
        tradeInfo.getStyleClass().add("bisq-easy-trade-state-headline");

        openTradeGuide = new Hyperlink(Res.get("bisqEasy.assistant.header.openTradeGuide"));

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

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

        openDisputeButton = new Button(Res.get("bisqEasy.openDispute"));
        openDisputeButton.getStyleClass().add("grey-transparent-outlined-button");

        VBox.setMargin(phaseHeadline, new Insets(0, 0, 20, 0));
        VBox.setMargin(openDisputeButton, new Insets(20, 0, 0, 0));
        Separator hLine = getHLine();
        VBox.setMargin(hLine, new Insets(40, 0, 0, 0));
        VBox phaseBox = new VBox(
                phaseHeadline,
                phase1HBox,
                getVLine(),
                phase2HBox,
                getVLine(),
                phase3HBox,
                getVLine(),
                phase4HBox,
                Spacer.fillVBox(),
                hLine,
                openDisputeButton
        );
        phaseBox.setMinWidth(300);
        phaseBox.setMaxWidth(phaseBox.getMinWidth());

        Label phaseInfoHeadline = new Label(Res.get("bisqEasy.assistant.tradeState.phaseInfo.headline"));
        phaseInfoHeadline.getStyleClass().add("bisq-easy-trade-state-sub-headline");

        phaseInfo = new Label(Res.get("bisqEasy.assistant.tradeState.phaseInfo.phase1"));
        phaseInfo.getStyleClass().add("bisq-easy-trade-state-info-text");
        phaseInfo.setWrapText(true);

        actionButton = new Button();
        actionButton.getStyleClass().add("outlined-button");

        Separator separator1 = getHLine();
        separator1.setOpacity(0.5);
        separator1.setPadding(new Insets(0, 0, -10, 0));

        Separator separator2 = getHLine();
        separator2.setOpacity(0.5);
        separator2.setPadding(new Insets(0, 0, -10, 0));

        VBox infoBox = new VBox(20, phaseInfoHeadline, phaseInfo, Spacer.fillVBox(), getHLine(), actionButton);

        HBox.setHgrow(infoBox, Priority.ALWAYS);
        HBox phaseAndInfoBox = new HBox(0, phaseBox, infoBox);
        phaseAndInfoBox.setPadding(new Insets(20));
        phaseAndInfoBox.getStyleClass().add("bisq-content-bg");

        VBox.setMargin(tradeInfo, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(tradeInfo, phaseAndInfoBox, nextButton);

        currentIndexListener = (observable, oldValue, newValue) -> applyProgress(newValue.intValue(), true);
    }

    private Separator getHLine() {
        Separator separator = new Separator();
        separator.setOpacity(0.4);
        return separator;
    }

    @Override
    protected void onViewAttached() {
        tradeInfo.textProperty().bind(model.getTradeInfo());
        phase2Label.textProperty().bind(model.getPhase2());
        phase3Label.textProperty().bind(model.getPhase3());
        actionButton.textProperty().bind(model.getActionButtonText());
        actionButton.visibleProperty().bind(model.getActionButtonVisible());
        actionButton.managedProperty().bind(model.getActionButtonVisible());
        openDisputeButton.visibleProperty().bind(model.getOpenDisputeButtonVisible());
        openDisputeButton.managedProperty().bind(model.getOpenDisputeButtonVisible());

        nextButton.setOnAction(e -> controller.onNext());
        actionButton.setOnAction(e -> controller.onAction());
        openDisputeButton.setOnAction(e -> controller.onOpenDispute());
        openTradeGuide.setOnAction(e -> controller.onOpenTradeGuide());

        model.getCurrentIndex().addListener(currentIndexListener);

        topPaneBoxVisibleSubscription = EasyBind.subscribe(model.getTopPaneBoxVisible(), visible -> {
           /* if (visible) {
                VBox.setMargin(buttons, new Insets(0, 0, 40, 0));
            } else {
                VBox.setMargin(buttons, new Insets(0, 0, 240, 0));
            }*/
        });

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

      /*  widthPin = EasyBind.subscribe(root.widthProperty(),
                w -> infoLabel.setWrappingWidth(w.doubleValue() - 30));*/
    }

    @Override
    protected void onViewDetached() {
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

        model.getCurrentIndex().removeListener(currentIndexListener);

        topPaneBoxVisibleSubscription.unsubscribe();
        activePhaseIndexPin.unsubscribe();
        // widthPin.unsubscribe();
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
