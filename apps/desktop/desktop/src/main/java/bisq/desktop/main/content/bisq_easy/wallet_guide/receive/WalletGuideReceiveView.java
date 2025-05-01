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

package bisq.desktop.main.content.bisq_easy.wallet_guide.receive;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.Carousel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletGuideReceiveView extends View<HBox, WalletGuideReceiveModel, WalletGuideReceiveController> {
    private final Button backButton, closeButton;
    private final Hyperlink link1, link2;
    private final Carousel imageCarousel;
    private final HBox indicators;

    public WalletGuideReceiveView(WalletGuideReceiveModel model, WalletGuideReceiveController controller) {
        super(new HBox(20), model, controller);

        VBox vBox = new VBox(20);
        vBox.setFillWidth(true);
        vBox.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("bisqEasy.walletGuide.receive.headline"));
        headline.getStyleClass().add("bisq-easy-trade-guide-headline");

        Text text = new Text(Res.get("bisqEasy.walletGuide.receive.content"));
        text.getStyleClass().add("bisq-easy-trade-guide-content");
        TextFlow content = new TextFlow(text);

        link1 = new Hyperlink(Res.get("bisqEasy.walletGuide.receive.link1"));
        link2 = new Hyperlink(Res.get("bisqEasy.walletGuide.receive.link2"));
        link1.setTooltip(new BisqTooltip("https://www.youtube.com/watch?v=NqY3wBhloH4"));
        link2.setTooltip(new BisqTooltip("https://www.youtube.com/watch?v=imMX7i4qpmg"));
        backButton = new Button(Res.get("action.back"));
        closeButton = new Button(Res.get("action.close"));
        closeButton.setDefaultButton(true);

        HBox buttons = new HBox(20, backButton, closeButton);

        VBox.setMargin(headline, new Insets(0, 0, -5, 0));
        VBox.setMargin(content, new Insets(0, 0, 5, 0));
        VBox.setMargin(link1, new Insets(-10, 0, -22.5, 0));
        VBox.setMargin(link2, new Insets(0, 0, 0, 0));
        vBox.getChildren().addAll(headline, content, link1, link2, buttons);

        // Right content pane - carousel
        // Get images and configure them to desired size
        ImageView image1 = ImageUtil.getImageViewById("blue-wallet-tx");
        ImageView image2 = ImageUtil.getImageViewById("blue-wallet-qr");

        // Set consistent dimensions for both images - smartphone-like aspect ratio
        double imageWidth = 250;
        double imageHeight = 430;

        image1.setFitWidth(imageWidth);
        image1.setFitHeight(imageHeight);
        image1.setPreserveRatio(true);

        image2.setFitWidth(imageWidth);
        image2.setFitHeight(imageHeight);
        image2.setPreserveRatio(true);

        // Create small indicator dots
        indicators = new HBox(6);
        indicators.setAlignment(Pos.CENTER);
        indicators.getStyleClass().add("carousel-indicators");

        // Configure carousel
        imageCarousel = new Carousel();
        imageCarousel.setTransitionDuration(Duration.millis(400));
        imageCarousel.setDisplayDuration(Duration.millis(3000));

        // Add images to carousel
        imageCarousel.addItem(image1);
        imageCarousel.addItem(image2);

        // Set up the indicator dots
        for (int i = 0; i < 2; i++) {
            StackPane dot = new StackPane();
            dot.getStyleClass().add("carousel-indicator");
            if (i == 0) {
                dot.getStyleClass().add("active");
            }

            final int index = i;
            dot.setOnMouseClicked(e -> {
                imageCarousel.goToIndex(index);
                e.consume();
            });

            indicators.getChildren().add(dot);
        }

        // Listen to index changes to update indicators
        imageCarousel.currentIndexProperty().addListener((obs, oldVal, newVal) -> {
            for (int i = 0; i < indicators.getChildren().size(); i++) {
                StackPane dot = (StackPane) indicators.getChildren().get(i);
                if (i == newVal.intValue()) {
                    if (!dot.getStyleClass().contains("active")) {
                        dot.getStyleClass().add("active");
                    }
                } else {
                    dot.getStyleClass().remove("active");
                }
            }
        });

        // Container for carousel and indicators
        VBox carouselContainer = new VBox(5);
        carouselContainer.setAlignment(Pos.CENTER);
        carouselContainer.getStyleClass().add("carousel-container");
        carouselContainer.getChildren().addAll(imageCarousel, indicators);
        carouselContainer.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(vBox, carouselContainer);
        root.setAlignment(Pos.TOP_CENTER);
    }

    @Override
    protected void onViewAttached() {
        closeButton.setOnAction(e -> controller.onClose());
        backButton.setOnAction(e -> controller.onBack());
        link1.setOnAction(e -> controller.onOpenLink1());
        link2.setOnAction(e -> controller.onOpenLink2());

        // Start the carousel
        imageCarousel.start();
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        backButton.setOnAction(null);
        link1.setOnAction(null);
        link2.setOnAction(null);

        // Stop the carousel when the view is detached
        imageCarousel.stop();
    }
}