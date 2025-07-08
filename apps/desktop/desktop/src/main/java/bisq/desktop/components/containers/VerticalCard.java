package bisq.desktop.components.containers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.InputStream;

@Slf4j
public class VerticalCard extends VBox {

    private static final int DEFAULT_IMAGE_WIDTH = 80;
    private static final int DEFAULT_IMAGE_HEIGHT = 80;
    private static final int DEFAULT_WIDTH = 200;
    private static final int SPACING = 20;
    private static final Insets CARD_PADDING = new Insets(30, 48, 44, 48);
    private static final Insets IMAGE_MARGIN = new Insets(0, 0, 0, 7);

    // Main constructor
    public VerticalCard(@Nullable String step,
                        @Nullable String imageSrc,
                        @Nullable String caption,
                        int imageWidth,
                        int imageHeight) {
        super(SPACING);
        setMinWidth(DEFAULT_WIDTH);
        setWidth(DEFAULT_WIDTH);
        setMaxWidth(DEFAULT_WIDTH);
        buildUI(step, imageSrc, caption, imageWidth, imageHeight);
    }

    // Overloaded constructor with default image size
    public VerticalCard(@Nullable String step,
                        @Nullable String imageSrc,
                        @Nullable String caption) {
        this(step, imageSrc, caption, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
    }

    private void buildUI(@Nullable String step,
                         @Nullable String imageSrc,
                         @Nullable String caption,
                         int imageWidth,
                         int imageHeight) {

        if (step != null && !step.isBlank()) {
            this.getChildren().add(createStepLabel(step));
        }

        if (imageSrc != null && !imageSrc.isBlank()) {
            VBox imageContainer = createImageContainer(imageSrc, imageWidth, imageHeight);
            if (imageContainer != null) {
                this.getChildren().add(imageContainer);
            }
        }

        if (caption != null && !caption.isBlank()) {
            this.getChildren().add(createCaptionLabel(caption));
        }

        this.setAlignment(Pos.TOP_CENTER);
        this.setPadding(CARD_PADDING);
        this.getStyleClass().add("bisq-card-bg");
    }

    private Label createStepLabel(String step) {
        Label label = new Label(step);
        label.getStyleClass().add("very-large-text");
        return label;
    }

    private VBox createImageContainer(String imageSrc, int imageWidth, int imageHeight) {
        Image image = loadImage(imageSrc);
        if (image == null) return null;

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(imageWidth);
        imageView.setFitHeight(imageHeight);

        VBox container = new VBox(SPACING, imageView);
        container.setAlignment(Pos.CENTER);
        VBox.setMargin(container, IMAGE_MARGIN);
        VBox.setVgrow(container, Priority.ALWAYS);
        return container;
    }

    private Label createCaptionLabel(String caption) {
        return new Label(caption);
    }

    private @Nullable Image loadImage(String src) {
        try (InputStream imageStream = getClass().getResourceAsStream(src)) {
            if (imageStream == null) {
                log.warn("Image not found: {}", src);
                return null;
            }
            return new Image(imageStream);
        } catch (Exception e) {
            log.error("Failed to load image: {}", src, e);
            return null;
        }
    }
}
