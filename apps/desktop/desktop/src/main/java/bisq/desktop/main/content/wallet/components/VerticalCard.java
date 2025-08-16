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

package bisq.desktop.main.content.wallet.components;

import bisq.desktop.common.utils.ImageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerticalCard extends VBox {
    private static final int DEFAULT_IMAGE_WIDTH = 140;
    private static final int DEFAULT_IMAGE_HEIGHT = 140;
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 270;
    private static final Insets CARD_PADDING = new Insets(20, 0, 30, 0);
    //private static final Insets IMAGE_MARGIN = new Insets(0, 0, 0, 7);

    // Main constructor
    public VerticalCard(String step,
                        String imageId,
                        String caption,
                        int imageWidth,
                        int imageHeight) {
        super();

        setMinWidth(DEFAULT_WIDTH);
        setMaxWidth(DEFAULT_WIDTH);

        setMinHeight(DEFAULT_HEIGHT);
        setMaxHeight(DEFAULT_HEIGHT);

        setAlignment(Pos.TOP_CENTER);
        //setPadding(CARD_PADDING);
        getStyleClass().add("bisq-card-bg");

       // buildUI(step, imageId, caption, imageWidth, imageHeight);

        Label stepLabel = new Label(step);
        stepLabel.getStyleClass().addAll("vertical-card-step");

        // VBox imageContainer = createImageContainer(imageId, imageWidth, imageHeight);
        ImageView imageView = ImageUtil.getImageViewById(imageId);
        // getChildren().add(imageContainer);

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().addAll("bisq-text-19", "text-fill-light-dimmed");

        VBox.setMargin(stepLabel, new Insets(-20,0,0,0));
        VBox.setMargin(captionLabel, new Insets(10,0,30,0));
        getChildren().addAll(stepLabel, imageView, captionLabel);
    }

    // Overloaded constructor with default image size
    public VerticalCard(String step,
                        String imageId,
                        String caption) {
        this(step, imageId, caption, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
    }

   /* private void buildUI(String step,
                         String imageId,
                         String caption,
                         int imageWidth,
                         int imageHeight) {

        Label stepLabel = new Label(step);
        stepLabel.getStyleClass().addAll("vertical-card-step");

        // VBox imageContainer = createImageContainer(imageId, imageWidth, imageHeight);
        ImageView imageView = ImageUtil.getImageViewById(imageId);
        // getChildren().add(imageContainer);

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().addAll("bisq-text-19", "text-fill-light-dimmed");

        getChildren().addAll(stepLabel, imageView, captionLabel);

        setAlignment(Pos.TOP_CENTER);
        setPadding(CARD_PADDING);
        getStyleClass().add("bisq-card-bg");
    }*/

   /* private VBox createImageContainer(String imageId, int imageWidth, int imageHeight) {
        ImageView imageView = ImageUtil.getImageViewById(imageId);
        imageView.setFitWidth(imageWidth);
        imageView.setFitHeight(imageHeight);

        VBox container = new VBox(0, imageView);
        container.setAlignment(Pos.CENTER);
        VBox.setMargin(container, IMAGE_MARGIN);
        VBox.setVgrow(container, Priority.ALWAYS);
        return container;
    }*/

}
