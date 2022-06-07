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
import bisq.common.data.Triple;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.primary.overlay.onboarding.Utils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;

@Slf4j
public class DirectionView extends View<VBox, DirectionModel, DirectionController> {
    private final Button nextButton;
    private final ToggleButton buyButton, sellButton;
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final Button skipButton;
    private Subscription directionSubscription;

    public DirectionView(DirectionModel model, DirectionController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("bisq-content-bg");

        Triple<HBox, Button, List<Label>> topPane = Utils.getTopPane();
        HBox topPaneBox = topPane.first();
        skipButton = topPane.second();
        List<Label> labelList = topPane.third();
        labelList.get(0).getStyleClass().add("bisq-text-white");

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

        nextButton = new Button(Res.get("next"));
        nextButton.setDefaultButton(true);

        VBox.setMargin(headLineLabel, new Insets(55, 0, 4, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 60, 0));
        VBox.setMargin(nextButton, new Insets(0, 0, 50, 0));
        root.getChildren().addAll(topPaneBox, headLineLabel, subtitleLabel, boxes, Spacer.fillVBox(), nextButton);
    }

    @Override
    protected void onViewAttached() {
        buyButton.disableProperty().bind(buyButton.selectedProperty());
        sellButton.disableProperty().bind(sellButton.selectedProperty());
        buyButton.setOnAction(evt -> controller.onSelect(DirectionModel.Direction.BUY));
        sellButton.setOnAction(evt -> controller.onSelect(DirectionModel.Direction.SELL));

        nextButton.setOnAction(e -> controller.onNext());
        skipButton.setOnAction(e -> controller.onSkip());
        directionSubscription = EasyBind.subscribe(model.getDirection(), direction -> {
            if (direction != null) {
                toggleGroup.selectToggle(direction == DirectionModel.Direction.BUY ? buyButton : sellButton);

            }
        });
    }

    @Override
    protected void onViewDetached() {
        buyButton.disableProperty().unbind();
        sellButton.disableProperty().unbind();
        buyButton.setOnAction(null);
        sellButton.setOnAction(null);
        nextButton.setOnAction(null);
        skipButton.setOnAction(null);
        directionSubscription.unsubscribe();
    }

    private Pair<VBox, ToggleButton> getBoxPair(String title, String info) {
        ToggleButton button = new ToggleButton(title);
        button.setToggleGroup(toggleGroup);
        button.getStyleClass().setAll("bisq-button-1");
        button.setAlignment(Pos.CENTER);
        int width = 250;
        button.setMinWidth(width);
        button.setMinHeight(125);

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

}
