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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_info.phase;

import bisq.common.data.Pair;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;

@Slf4j
public class TradeInfoPhaseView extends View<VBox, TradeInfoPhaseModel, TradeInfoPhaseController> {
    private static final double TOP_PANE_HEIGHT = 55;
    private static final double OPACITY = 0.35;

    private final List<Label> navigationProgressLabelList;
    private final HBox topPaneBox;
    private final Button confirmButton, openDisputeButton;
    private final HBox buttons;
    private final Hyperlink learnMore;
    private final Text content;
    private Subscription widthPin;
    private Subscription topPaneBoxVisibleSubscription;
    private final ChangeListener<Number> currentIndexListener;

    public TradeInfoPhaseView(TradeInfoPhaseModel model,
                              TradeInfoPhaseController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("tradeInfo.phase.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        content = new Text(Res.get("tradeInfo.phase.content"));
        content.getStyleClass().addAll("bisq-text-13", "bisq-line-spacing-01");

        Pair<HBox, List<Label>> topPane = getTopPane();
        topPaneBox = topPane.getFirst();
        navigationProgressLabelList = topPane.getSecond();

        confirmButton = new Button();
        confirmButton.setDefaultButton(true);

        openDisputeButton = new Button(Res.get("bisqEasy.openDispute"));
        buttons = new HBox(10, confirmButton, openDisputeButton);
        buttons.setAlignment(Pos.CENTER);

        learnMore = new Hyperlink(Res.get("user.reputation.learnMore"));

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(learnMore, new Insets(0, 0, 10, 0));
        root.getChildren().addAll(headline, topPaneBox, content, learnMore, buttons);

        currentIndexListener = (observable, oldValue, newValue) -> applyProgress(newValue.intValue(), true);
    }

    @Override
    protected void onViewAttached() {
        confirmButton.textProperty().bind(model.getConfirmButtonText());
        openDisputeButton.visibleProperty().bind(model.getOpenDisputeButtonVisible());
        openDisputeButton.managedProperty().bind(model.getOpenDisputeButtonVisible());

        confirmButton.setOnAction(e -> controller.onNext());
        openDisputeButton.setOnAction(e -> controller.onOpenDispute());
        learnMore.setOnAction(e -> controller.onLearnMore());

        model.getCurrentIndex().addListener(currentIndexListener);

        topPaneBoxVisibleSubscription = EasyBind.subscribe(model.getTopPaneBoxVisible(), visible -> {
            if (visible) {
                VBox.setMargin(buttons, new Insets(0, 0, 40, 0));
            } else {
                VBox.setMargin(buttons, new Insets(0, 0, 240, 0));
            }
        });
        widthPin = EasyBind.subscribe(root.widthProperty(),
                w -> content.setWrappingWidth(w.doubleValue() - 30));
    }

    @Override
    protected void onViewDetached() {
        confirmButton.textProperty().unbind();
        openDisputeButton.visibleProperty().unbind();
        openDisputeButton.managedProperty().unbind();

        confirmButton.setOnAction(null);
        openDisputeButton.setOnAction(null);
        learnMore.setOnAction(null);

        model.getCurrentIndex().removeListener(currentIndexListener);

        topPaneBoxVisibleSubscription.unsubscribe();
        widthPin.unsubscribe();
    }

    private Pair<HBox, List<Label>> getTopPane() {
        Label negotiation = getTopPaneLabel(Res.get("tradeInfo.phase.negotiation"));
        Label fiat = getTopPaneLabel(Res.get("tradeInfo.phase.fiat"));
        Label btc = getTopPaneLabel(Res.get("tradeInfo.phase.btc"));
        Label complete = getTopPaneLabel(Res.get("tradeInfo.phase.complete"));


        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.setId("onboarding-top-panel");
        hBox.setMinHeight(TOP_PANE_HEIGHT);
        hBox.setPadding(new Insets(0, 20, 0, 50));
        hBox.getChildren().addAll(Spacer.fillHBox(),
                negotiation,
                getSeparator(),
                fiat,
                getSeparator(),
                btc,
                getSeparator(),
                complete,
                Spacer.fillHBox());

        return new Pair<>(hBox, List.of(negotiation, fiat, btc, complete));
    }

    private Separator getSeparator() {
        Separator line = new Separator();
        line.setPrefWidth(30);
        return line;
    }

    private Label getTopPaneLabel(String text) {
        Label label = new Label(text.toUpperCase());
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().addAll("bisq-text-14");

        label.setOpacity(OPACITY);
        return label;
    }

    private void applyProgress(int progressIndex, boolean delay) {
        if (progressIndex < navigationProgressLabelList.size()) {
            navigationProgressLabelList.forEach(label -> label.setOpacity(OPACITY));
            Label label = navigationProgressLabelList.get(progressIndex);
            if (delay) {
                UIScheduler.run(() -> Transitions.fade(label, OPACITY, 1, Transitions.DEFAULT_DURATION / 2))
                        .after(Transitions.DEFAULT_DURATION / 2);
            } else {
                label.setOpacity(1);
            }
        }
    }
}
