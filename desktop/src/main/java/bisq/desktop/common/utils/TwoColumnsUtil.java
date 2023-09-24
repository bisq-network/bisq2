package bisq.desktop.common.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class TwoColumnsUtil {
    public static void setColumnConstraints50percent(GridPane pane) {
        setGridPaneTwoColumnsConstraints(pane, 50, 50);
    }

    public static void setGridPaneTwoColumnsConstraints(GridPane pane, int percentageCol1, int percentageCol2) {
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(percentageCol1);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(percentageCol2);
        pane.getColumnConstraints().addAll(col1, col2);
    }

    /**
     * Set the layout for the grid pane.
     */
    public static GridPane getTwoColumnsGridPane(int hGap, int vGap, Insets gridPadding, int col1PercentWidth, int col2PercentWidth) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(hGap);
        gridPane.setVgap(vGap);
        gridPane.setPadding(gridPadding);
        setGridPaneTwoColumnsConstraints(gridPane, col1PercentWidth, col2PercentWidth);
        return gridPane;
    }

    /**
     *  Get icon and text as a label. See example at Dashboard, multiple trade protocols box
     */
    public static HBox getIconAndText(String labelStyleClass, String text, String imageId) {
        Label label = new Label(text);
        label.getStyleClass().add(labelStyleClass);
        label.setWrapText(true);
        ImageView bulletPoint = ImageUtil.getImageViewById(imageId);
        HBox.setMargin(bulletPoint, new Insets(-3, 0, 0, 4));
        HBox hBox = new HBox(15, bulletPoint, label);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return hBox;
    }

    /**
     * Column box using the standard style. See example at Dashboard, multiple trade protocols box
     */
    public static void fillColumnStandardStyle(GridPane gridPane,
                                          int columnIndex,
                                          Button button,
                                          String headline,
                                          String headlineImageId,
                                          String info) {
        String groupPaneStyleClass = "bisq-box-1";
        String headlineLabelStyleClass = "bisq-text-headline-2";
        String infoLabelStyleClass = "bisq-text-3";
        String buttonStyleClass = "large-button";
        fillColumn(gridPane,
                columnIndex,
                button,
                buttonStyleClass,
                headline,
                headlineLabelStyleClass,
                headlineImageId,
                info,
                infoLabelStyleClass,
                groupPaneStyleClass);
    }

    /**
     * Column box using a custom style. See example at Bisq Easy, best for beginners section
     */
    public static void fillColumn(GridPane gridPane,
                                     int columnIndex,
                                     Button button,
                                     String buttonStyleClass,
                                     String headline,
                                     String headlineStyleClass,
                                     String headlineImageId,
                                     String info,
                                     String infoLabelStyleClass,
                                     String groupPaneStyleClass) {

        Pane group = new Pane();
        group.getStyleClass().add(groupPaneStyleClass);
        GridPane.setMargin(group, new Insets(-36, -48, -44, -48));

        gridPane.add(group, columnIndex, 0, 1, 3);

        Label headlineLabel = new Label(headline, ImageUtil.getImageViewById(headlineImageId));
        headlineLabel.setGraphicTextGap(16.0);
        headlineLabel.getStyleClass().add(headlineStyleClass);
        headlineLabel.setWrapText(true);
        GridPane.setMargin(headlineLabel, new Insets(0, 0, 10, 0));
        gridPane.add(headlineLabel, columnIndex, 0);

        Text infoLabelText = new Text(info);
        infoLabelText.getStyleClass().add(infoLabelStyleClass);
        TextFlow infoLabel = new TextFlow(infoLabelText);
        gridPane.add(infoLabel, columnIndex, 1);

        button.getStyleClass().add(buttonStyleClass);
        button.setMaxWidth(Double.MAX_VALUE);
        GridPane.setMargin(button, new Insets(20, 0, 0, 0));
        gridPane.add(button, columnIndex, 2);
    }
}
