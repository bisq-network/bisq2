package bisq.desktop.main.content.wallet.components;

import bisq.common.util.StringUtils;
import bisq.desktop.common.utils.ImageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerticalCard extends VBox {

    private static final int DEFAULT_IMAGE_WIDTH = 80;
    private static final int DEFAULT_IMAGE_HEIGHT = 80;
    private static final int DEFAULT_WIDTH = 200;
    private static final int SPACING = 30;
    private static final Insets CARD_PADDING = new Insets(30, 40, 40, 40);
    private static final Insets IMAGE_MARGIN = new Insets(0, 0, 0, 7);

    // Main constructor
    public VerticalCard(String step,
                        String imageSrc,
                        String caption,
                        int imageWidth,
                        int imageHeight) {
        super(SPACING);
        setMinWidth(DEFAULT_WIDTH);
        setWidth(DEFAULT_WIDTH);
        setMaxWidth(DEFAULT_WIDTH);
        buildUI(step, imageSrc, caption, imageWidth, imageHeight);
    }

    // Overloaded constructor with default image size
    public VerticalCard(String step,
                        String imageSrc,
                        String caption) {
        this(step, imageSrc, caption, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
    }

    private void buildUI(String step,
                         String imageSrc,
                         String caption,
                         int imageWidth,
                         int imageHeight) {

        Label label = new Label(step);
        label.getStyleClass().addAll("very-large-text-2", "thin-text");
        getChildren().add(label);

        VBox imageContainer = createImageContainer(imageSrc, imageWidth, imageHeight);
        getChildren().add(imageContainer);

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().addAll("bisq-text-19", "text-fill-light-dimmed");
        getChildren().add(captionLabel);

        setAlignment(Pos.TOP_CENTER);
        setPadding(CARD_PADDING);
        getStyleClass().add("bisq-card-bg");
    }

    private VBox createImageContainer(String imageId, int imageWidth, int imageHeight) {
        ImageView imageView = ImageUtil.getImageViewById(imageId);
        imageView.setFitWidth(imageWidth);
        imageView.setFitHeight(imageHeight);

        VBox container = new VBox(SPACING, imageView);
        container.setAlignment(Pos.CENTER);
        VBox.setMargin(container, IMAGE_MARGIN);
        VBox.setVgrow(container, Priority.ALWAYS);
        return container;
    }

}
