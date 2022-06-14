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

package bisq.desktop.primary.overlay.onboarding.offer.direction;

import bisq.common.data.Pair;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class DirectionView extends View<StackPane, DirectionModel, DirectionController> {
    private final ToggleButton buyButton, sellButton;
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final VBox reputationInfo;
    private final VBox content;
    private Subscription directionSubscription, showReputationInfoPin;
    private Button gainReputationButton, withoutReputationButton, closeButton;

    public DirectionView(DirectionModel model, DirectionController controller) {
        super(new StackPane(), model, controller);

        content = new VBox();
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("bisq-content-bg");

        Label headLineLabel = new Label(Res.get("onboarding.direction.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.direction.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        Pair<VBox, ToggleButton> buyPair = getBoxPair(Res.get("onboarding.direction.buy"), Res.get("onboarding.direction.buy.info"));
        VBox buyBox = buyPair.first();
        buyButton = buyPair.second();

        Pair<VBox, ToggleButton> sellPair = getBoxPair(Res.get("onboarding.direction.sell"), Res.get("onboarding.direction.sell.info"));
        VBox sellBox = sellPair.first();
        sellButton = sellPair.second();

        HBox boxes = new HBox(25, buyBox, sellBox);
        boxes.setAlignment(Pos.CENTER);

        VBox.setMargin(headLineLabel, new Insets(44, 0, 2, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 53, 0));
        content.getChildren().addAll(headLineLabel, subtitleLabel, boxes);

        reputationInfo = new VBox();
        reputationInfo.setVisible(false);
        setupReputationInfo();

        StackPane.setMargin(reputationInfo, new Insets(-55, 0, 0, 0));
        root.getChildren().addAll(content, reputationInfo);
    }

    @Override
    protected void onViewAttached() {
        Transitions.removeEffect(content);
        buyButton.disableProperty().bind(buyButton.selectedProperty());
        sellButton.disableProperty().bind(sellButton.selectedProperty());

        buyButton.setOnAction(evt -> controller.onSelectDirection(Direction.BUY));
        sellButton.setOnAction(evt -> controller.onSelectDirection(Direction.SELL));
        gainReputationButton.setOnAction(evt -> controller.onGainReputation());
        withoutReputationButton.setOnAction(evt -> controller.onIgnoreReputation());
        closeButton.setOnAction(evt -> controller.onCloseReputationInfo());

        directionSubscription = EasyBind.subscribe(model.getDirection(), direction -> {
            if (direction != null) {
                toggleGroup.selectToggle(direction == Direction.BUY ? buyButton : sellButton);
            }
        });

        showReputationInfoPin = EasyBind.subscribe(model.getShowReputationInfo(),
                showReputationInfo -> {
                    if (showReputationInfo) {
                        Transitions.blurStrong(content, 0);
                        reputationInfo.setVisible(true);
                        reputationInfo.setOpacity(1);
                        Transitions.slideInTop(reputationInfo, 450);
                    } else {
                        Transitions.removeEffect(content);
                        if (reputationInfo.isVisible()) {
                            Transitions.fadeOut(reputationInfo, Transitions.DEFAULT_DURATION / 2, () -> {
                                reputationInfo.setVisible(false);
                            });
                        }
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        buyButton.disableProperty().unbind();
        sellButton.disableProperty().unbind();

        buyButton.setOnAction(null);
        sellButton.setOnAction(null);
        gainReputationButton.setOnAction(null);
        withoutReputationButton.setOnAction(null);
        closeButton.setOnAction(null);

        directionSubscription.unsubscribe();
        showReputationInfoPin.unsubscribe();
    }

    private Pair<VBox, ToggleButton> getBoxPair(String title, String info) {
        ToggleButton button = new ToggleButton(title);
        button.setToggleGroup(toggleGroup);
        button.getStyleClass().setAll("bisq-button-1");
        button.setAlignment(Pos.CENTER);
        int width = 235;
        button.setMinWidth(width);
        button.setMinHeight(112);

        Label infoLabel = new Label(info);
        infoLabel.getStyleClass().add("bisq-text-3");
        infoLabel.setMaxWidth(width);
        infoLabel.setWrapText(true);
        infoLabel.setTextAlignment(TextAlignment.CENTER);
        infoLabel.setAlignment(Pos.CENTER);

        VBox vBox = new VBox(8, button, infoLabel);
        vBox.setAlignment(Pos.CENTER);

        return new Pair<>(vBox, button);
    }

    private void setupReputationInfo() {
        double width = 700;
        reputationInfo.setAlignment(Pos.TOP_CENTER);
        reputationInfo.setMaxWidth(width);
        reputationInfo.setId("sellBtcWarning");
        reputationInfo.setVisible(false);

        Label headLineLabel = new Label(Res.get("onboarding.direction.feedback.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.direction.feedback.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMaxWidth(width - 200);
        subtitleLabel.getStyleClass().addAll("bisq-text-13", "wrap-text");

        gainReputationButton = new Button(Res.get("onboarding.direction.feedback.gainReputation"));
        gainReputationButton.setDefaultButton(true);

        withoutReputationButton = new Button(Res.get("onboarding.direction.feedback.tradeWithoutReputation"));

        closeButton = BisqIconButton.createIconButton("close-round");

        HBox buttons = new HBox(7, withoutReputationButton, gainReputationButton);
        buttons.setAlignment(Pos.CENTER);

        VBox.setMargin(closeButton, new Insets(3, 0, 0, width - 35));
        VBox.setMargin(headLineLabel, new Insets(-10, 0, 30, 0));
        VBox.setMargin(buttons, new Insets(50, 0, 30, 0));
        reputationInfo.getChildren().addAll(closeButton, headLineLabel, subtitleLabel, buttons);
    }
}
