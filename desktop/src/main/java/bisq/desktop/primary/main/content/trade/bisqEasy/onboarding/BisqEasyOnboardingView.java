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

package bisq.desktop.primary.main.content.trade.bisqEasy.onboarding;

import bisq.common.data.Pair;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.Switch;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyOnboardingView extends View<GridPane, BisqEasyOnboardingModel, BisqEasyOnboardingController> {
    private static final int PADDING = 20;

    private final Button createOfferButton, openChatButton;
    private final Switch dontShowAgain;

    public BisqEasyOnboardingView(BisqEasyOnboardingModel model, BisqEasyOnboardingController controller) {
        super(new GridPane(), model, controller);

        root.setHgap(PADDING);
        root.setVgap(PADDING);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        root.getColumnConstraints().addAll(col1, col2);

        Label headlineLabel = new Label(Res.get("bisqEasy.onboarding.headline"));
        headlineLabel.getStyleClass().addAll("bisq-text-headline-4");
        headlineLabel.setWrapText(true);
        root.add(headlineLabel, 0, 0, 2, 1);

        Pair<VBox, Button> leftBoxPair = getWidgetBox(
                Res.get("bisqEasy.onboarding.left.headline"),
                Res.get("bisqEasy.onboarding.left.content1"),
                Res.get("bisqEasy.onboarding.left.content2"),
                Res.get("bisqEasy.onboarding.left.content3"),
                "onboarding-2-offer",
                "onboarding-2-chat",
                "onboarding-1-reputation",
                Res.get("bisqEasy.onboarding.left.button")
        );
        createOfferButton = leftBoxPair.getSecond();
        createOfferButton.setDefaultButton(true);
        root.add(leftBoxPair.getFirst(), 0, 1, 1, 1);

        Pair<VBox, Button> rightBoxPair = getWidgetBox(
                Res.get("bisqEasy.onboarding.right.headline"),
                Res.get("bisqEasy.onboarding.right.content1"),
                Res.get("bisqEasy.onboarding.right.content2"),
                Res.get("bisqEasy.onboarding.right.content3"),
                "onboarding-2-payment",
                "onboarding-2-chat",
                "onboarding-3-method",
                Res.get("bisqEasy.onboarding.right.button")
        );
        openChatButton = rightBoxPair.getSecond();
        root.add(rightBoxPair.getFirst(), 1, 1, 1, 1);

        dontShowAgain = new Switch(Res.get("dontShowAgain"));
        root.add(dontShowAgain, 0, 2, 2, 1);
    }

    @Override
    protected void onViewAttached() {
        dontShowAgain.setSelected(false);
        createOfferButton.setOnAction(e -> controller.onCreateOffer());
        openChatButton.setOnAction(e -> controller.onOpenChat());
        dontShowAgain.setOnAction(e -> controller.onDontShowAgain());
    }

    @Override
    protected void onViewDetached() {
        createOfferButton.setOnAction(null);
        openChatButton.setOnAction(null);
    }

    private Pair<VBox, Button> getWidgetBox(String headline,
                                            String content1,
                                            String content2,
                                            String content3,
                                            String imageId1,
                                            String imageId2,
                                            String imageId3,
                                            String buttonLabel) {
        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");
        headlineLabel.setWrapText(true);

        Button button = new Button(buttonLabel);
        button.getStyleClass().add("large-button");
        button.setMaxWidth(Double.MAX_VALUE);

        VBox.setMargin(headlineLabel, new Insets(0, 0, 10, 0));
        VBox.setMargin(button, new Insets(20, 0, 0, 0));
        VBox vBox = new VBox(16,
                headlineLabel,
                getIconAndText(content1, imageId1),
                getIconAndText(content2, imageId2),
                getIconAndText(content3, imageId3),
                button);
        vBox.getStyleClass().add("bisq-box-1");
        vBox.setPadding(new Insets(36, 48, 52, 48));
        return new Pair<>(vBox, button);
    }

    private HBox getIconAndText(String text, String imageId) {
        Label label = new Label(text);
        label.setId("bisq-easy-onboarding-label");
        label.setWrapText(true);
        ImageView bulletPoint = ImageUtil.getImageViewById(imageId);
        HBox.setMargin(bulletPoint, new Insets(-2, 0, 0, 4));
        HBox hBox = new HBox(15, bulletPoint, label);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return hBox;
    }
}
