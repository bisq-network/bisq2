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

import bisq.desktop.common.Icons;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
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

        root.setPadding(new Insets(30, 0, -44, 0));
        root.setHgap(PADDING);
        root.setVgap(10);
        root.setMinWidth(780);
        GridPaneUtil.setColumnConstraints50percent(root);

        //First row
        addTopWidgetBox();

        //Second row
        Insets gridPaneInsets = new Insets(36, 48, 44, 48);
        GridPane gridPane = GridPaneUtil.getTwoColumnsGridPane(116, 15, gridPaneInsets, 50, 50);
        root.add(gridPane, 0, 2, 2, 1);

        String groupPaneStyleClass = "bisq-easy-onboarding-small-box";
        String headlineLabelStyleClass = "bisq-easy-onboarding-small-box-headline";
        String infoLabelStyleClass = "bisq-easy-onboarding-small-box-text";
        String buttonStyleClass = "large-button";

        startTradingButton = new Button(Res.get("bisqEasy.onboarding.left.button"));
        GridPaneUtil.fillColumn(gridPane,
                0,
                startTradingButton,
                buttonStyleClass,
                Res.get("bisqEasy.onboarding.left.headline"),
                headlineLabelStyleClass,
                "bisq-easy",
                Res.get("bisqEasy.onboarding.left.info"),
                infoLabelStyleClass,
                groupPaneStyleClass);

        openChatButton = new Button(Res.get("bisqEasy.onboarding.right.button"));
        GridPaneUtil.fillColumn(gridPane,
                1,
                openChatButton,
                buttonStyleClass,
                Res.get("bisqEasy.onboarding.right.headline"),
                headlineLabelStyleClass,
                "fiat-btc",
                Res.get("bisqEasy.onboarding.right.info"),
                infoLabelStyleClass,
                groupPaneStyleClass);
    }

    @Override
    protected void onViewAttached() {
        videoSeenPin = EasyBind.subscribe(model.getVideoSeen(), videoSeen -> {
            startTradingButton.setDefaultButton(videoSeen);
            watchVideoButton.setDefaultButton(!videoSeen);
        });

        startTradingButton.setOnAction(e -> controller.onOpenTradeWizard());
        openChatButton.setOnAction(e -> controller.onOpenOfferbook());
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
        GridPane gridPane = GridPaneUtil.getTwoColumnsGridPane(48, 15, new Insets(30, 48, 44, 48), 50, 50);
        gridPane.getStyleClass().add("bisq-easy-onboarding-big-box");
        root.add(gridPane, 0, 0, 2, 1);

        Label headlineLabel = new Label(Res.get("bisqEasy.onboarding.top.headline"));
        headlineLabel.getStyleClass().add("bisq-easy-onboarding-big-box-headline");
        headlineLabel.setWrapText(true);
        gridPane.add(headlineLabel, 0, 0, 2, 1);

        String lineStyleClass = "bisq-easy-onboarding-big-box-bullet-point";
        HBox line1 = GridPaneUtil.getIconAndText(lineStyleClass, Res.get("bisqEasy.onboarding.top.content1"), "thumbs-up");
        HBox line2 = GridPaneUtil.getIconAndText(lineStyleClass, Res.get("bisqEasy.onboarding.top.content2"), "onboarding-2-payment");
        HBox line3 = GridPaneUtil.getIconAndText(lineStyleClass, Res.get("bisqEasy.onboarding.top.content3"), "onboarding-2-chat");
        VBox vBox = new VBox(15, Spacer.fillVBox(), line1, line2, line3, Spacer.fillVBox());
        gridPane.add(vBox, 0, 1);

        videoImage = ImageUtil.getImageViewById("video");
        videoImage.setCursor(Cursor.HAND);
        Tooltip.install(videoImage, new BisqTooltip(Res.get("bisqEasy.onboarding.watchVideo.tooltip")));
        GridPane.setHalignment(videoImage, HPos.CENTER);
        gridPane.add(videoImage, 1, 1);

        openTradeGuideButton = new Button(Res.get("bisqEasy.onboarding.openTradeGuide"));
        openTradeGuideButton.getStyleClass().add("super-large-button");
        openTradeGuideButton.setMaxWidth(Double.MAX_VALUE);
        GridPane.setMargin(openTradeGuideButton, new Insets(10, 0, 0, 0));
        gridPane.add(openTradeGuideButton, 0, 2);

        Label icon = Icons.getIcon(AwesomeIcon.YOUTUBE_PLAY, "26");
        watchVideoButton = new Button(Res.get("bisqEasy.onboarding.watchVideo"), icon);
        watchVideoButton.setGraphicTextGap(10);
        watchVideoButton.getStyleClass().add("super-large-button");
        watchVideoButton.setMaxWidth(Double.MAX_VALUE);
        watchVideoButton.setTooltip(new BisqTooltip(Res.get("bisqEasy.onboarding.watchVideo.tooltip")));
        GridPane.setMargin(watchVideoButton, new Insets(10, 0, 0, 0));
        gridPane.add(watchVideoButton, 1, 2);
    }

}
