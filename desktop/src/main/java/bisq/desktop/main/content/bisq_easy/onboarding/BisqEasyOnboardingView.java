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

package bisq.desktop.main.content.bisq_easy.onboarding;

import bisq.common.data.Pair;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BisqEasyOnboardingView extends View<GridPane, BisqEasyOnboardingModel, BisqEasyOnboardingController> {
    private static final int PADDING = 20;

    private Button watchVideoButton, openTradeGuideButton;
    private final Button startTradingButton, openChatButton;
    private ImageView videoImage;
    private Subscription videoSeenPin;

    public BisqEasyOnboardingView(BisqEasyOnboardingModel model, BisqEasyOnboardingController controller) {
        super(new GridPane(), model, controller);

        root.setPadding(new Insets(20, 0, 0, 0));
        root.setHgap(PADDING);
        root.setVgap(PADDING);
        root.setMinWidth(780);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        root.getColumnConstraints().addAll(col1, col2);

        addTopWidgetBox();

        Label startTradingHeadlineLabel = new Label(Res.get("bisqEasy.onboarding.startTrading.headline"));
        startTradingHeadlineLabel.getStyleClass().add("bisq-text-headline-4");
        startTradingHeadlineLabel.setWrapText(true);
        GridPane.setMargin(startTradingHeadlineLabel, new Insets(10, 48, -5, 48));
        root.add(startTradingHeadlineLabel, 0, 1, 2, 1);

        GridPane gridPane = getWidgetBoxGridPane();
        root.add(gridPane, 0, 2, 2, 1);

        startTradingButton = new Button(Res.get("bisqEasy.onboarding.startTrading"));
        fillWidgetBox(gridPane,
                0,
                startTradingButton,
                Res.get("bisqEasy.onboarding.left.headline"),
                Res.get("bisqEasy.onboarding.left.content1"),
                Res.get("bisqEasy.onboarding.left.content2"),
                Res.get("bisqEasy.onboarding.left.content3"),
                "onboarding-2-offer",
                "onboarding-1-reputation",
                "onboarding-2-payment"
        );

        openChatButton = new Button(Res.get("bisqEasy.onboarding.openChat"));
        fillWidgetBox(gridPane,
                1,
                openChatButton,
                Res.get("bisqEasy.onboarding.right.headline"),
                Res.get("bisqEasy.onboarding.right.content1"),
                Res.get("bisqEasy.onboarding.right.content2"),
                Res.get("bisqEasy.onboarding.right.content3"),
                "onboarding-1-reputation",
                "onboarding-2-offer",
                "onboarding-2-payment"
        );
    }

    @Override
    protected void onViewAttached() {
        videoSeenPin = EasyBind.subscribe(model.getVideoSeen(), videoSeen -> {
            startTradingButton.setDefaultButton(videoSeen);
            watchVideoButton.setDefaultButton(!videoSeen);
        });

        startTradingButton.setOnAction(e -> controller.onCreateOffer());
        openChatButton.setOnAction(e -> controller.onOpenChat());
        openTradeGuideButton.setOnAction(e -> controller.onOpenTradeGuide());
        watchVideoButton.setOnMouseClicked(e -> controller.onPlayVideo());
        videoImage.setOnMouseClicked(e -> controller.onPlayVideo());
    }

    @Override
    protected void onViewDetached() {
        videoSeenPin.unsubscribe();

        startTradingButton.setOnAction(null);
        openChatButton.setOnAction(null);
        openTradeGuideButton.setOnAction(null);
        watchVideoButton.setOnAction(null);
        videoImage.setOnMouseClicked(null);
    }

    private void addTopWidgetBox() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(48);
        gridPane.setVgap(15);
        gridPane.getStyleClass().add("bisq-box-2");
        gridPane.setPadding(new Insets(30, 48, 44, 48));
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        gridPane.getColumnConstraints().addAll(col1, col2);
        root.add(gridPane, 0, 0, 2, 1);

        Label headlineLabel = new Label(Res.get("bisqEasy.onboarding.top.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-4");
        headlineLabel.setWrapText(true);
        GridPane.setMargin(headlineLabel, new Insets(0, 0, 10, 0));
        gridPane.add(headlineLabel, 0, 0, 2, 1);

        HBox line1 = getIconAndText(Res.get("bisqEasy.onboarding.top.content1"), "onboarding-bisq-easy");
        HBox line2 = getIconAndText(Res.get("bisqEasy.onboarding.top.content2"), "onboarding-trade");
        HBox line3 = getIconAndText(Res.get("bisqEasy.onboarding.top.content3"), "onboarding-1-easy");
        VBox vBox = new VBox(15, Spacer.fillVBox(), line1, line2, line3, Spacer.fillVBox());
        gridPane.add(vBox, 0, 1);

        videoImage = ImageUtil.getImageViewById("video");
        videoImage.setCursor(Cursor.HAND);
        Tooltip.install(videoImage, new Tooltip(Res.get("bisqEasy.onboarding.watchVideo.tooltip")));
        GridPane.setHalignment(videoImage, HPos.CENTER);
        gridPane.add(videoImage, 1, 1);

        openTradeGuideButton = new Button(Res.get("bisqEasy.onboarding.openTradeGuide"));
        openTradeGuideButton.getStyleClass().add("super-large-button");
        openTradeGuideButton.setMaxWidth(Double.MAX_VALUE);
        GridPane.setMargin(openTradeGuideButton, new Insets(10, 0, 0, 0));
        gridPane.add(openTradeGuideButton, 0, 2);

        watchVideoButton = new Button(Res.get("bisqEasy.onboarding.watchVideo"));
        watchVideoButton.getStyleClass().add("super-large-button");
        watchVideoButton.setMaxWidth(Double.MAX_VALUE);
        watchVideoButton.setTooltip(new Tooltip(Res.get("bisqEasy.onboarding.watchVideo.tooltip")));
        GridPane.setMargin(watchVideoButton, new Insets(10, 0, 0, 0));
        gridPane.add(watchVideoButton, 1, 2);
    }

    private GridPane getWidgetBoxGridPane() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(116);
        gridPane.setVgap(15);
        gridPane.setPadding(new Insets(36, 48, 52, 48));
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        gridPane.getColumnConstraints().addAll(col1, col2);
        return gridPane;
    }

    private void fillWidgetBox(GridPane gridPane,
                               int columnIndex,
                               Button button,
                               String headline,
                               String content1,
                               String content2,
                               String content3,
                               String imageId1,
                               String imageId2,
                               String imageId3) {
        Pane group = new Pane();
        group.getStyleClass().add("bisq-box-1");
        if (columnIndex == 0) {
            GridPane.setMargin(group, new Insets(-36, -48, -52, -48));
        } else {
            GridPane.setMargin(group, new Insets(-36, -48, -52, -48));
        }
        gridPane.add(group, columnIndex, 0, 1, 5);

        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().addAll("bisq-text-headline-2");
        headlineLabel.setWrapText(true);
        GridPane.setMargin(headlineLabel, new Insets(0, 0, 10, 0));
        gridPane.add(headlineLabel, columnIndex, 0);

        HBox line1 = getIconAndText(content1, imageId1);
        gridPane.add(line1, columnIndex, 1);
        HBox line2 = getIconAndText(content2, imageId2);
        gridPane.add(line2, columnIndex, 2);
        HBox line3 = getIconAndText(content3, imageId3);
        gridPane.add(line3, columnIndex, 3);

        button.getStyleClass().add("large-button");
        button.setMaxWidth(Double.MAX_VALUE);
        GridPane.setMargin(button, new Insets(20, 0, 0, 0));
        gridPane.add(button, columnIndex, 4);
    }

    private Pair<VBox, Button> getWidgetBox1(String headline,
                                             String content1,
                                             String content2,
                                             String content3,
                                             String imageId1,
                                             String imageId2,
                                             String imageId3,
                                             String buttonLabel) {
        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().addAll("bisq-text-headline-2");
        headlineLabel.setWrapText(true);

        Button button = new Button(buttonLabel);
        button.getStyleClass().add("large-button");
        button.setMaxWidth(Double.MAX_VALUE);

        VBox.setMargin(headlineLabel, new Insets(0, 0, 10, 0));
        VBox.setMargin(button, new Insets(20, 0, 0, 0));
        VBox vBox = new VBox(15,
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
