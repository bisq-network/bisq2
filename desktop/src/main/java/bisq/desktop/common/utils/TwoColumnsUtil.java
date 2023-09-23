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
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class TwoColumnsUtil {
    public static void setColumnConstraints(GridPane pane) {
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        pane.getColumnConstraints().addAll(col1, col2);
    }

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
        if (columnIndex == 0) {
            GridPane.setMargin(group, new Insets(-36, -48, -44, -48));
        } else {
            GridPane.setMargin(group, new Insets(-36, -48, -44, -48));
        }
        gridPane.add(group, columnIndex, 0, 1, 3);

        Label headlineLabel = new Label(headline, ImageUtil.getImageViewById(headlineImageId));
        headlineLabel.setGraphicTextGap(16.0);
        headlineLabel.getStyleClass().add(headlineStyleClass);
        headlineLabel.setWrapText(true);
        GridPane.setMargin(headlineLabel, new Insets(0, 0, 10, 0));
        gridPane.add(headlineLabel, columnIndex, 0);

        Label infoLabel = new Label(info);
        infoLabel.getStyleClass().add(infoLabelStyleClass);
        infoLabel.setWrapText(true);
        gridPane.add(infoLabel, columnIndex, 1);

        button.getStyleClass().add(buttonStyleClass);
        button.setMaxWidth(Double.MAX_VALUE);
        GridPane.setMargin(button, new Insets(20, 0, 0, 0));
        gridPane.add(button, columnIndex, 2);
    }

    public static GridPane getWidgetBoxGridPane(int hGap, int vGap, Insets gridPadding, int col1PercentWidth, int col2PercentWidth) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(hGap);
        gridPane.setVgap(vGap);
        gridPane.setPadding(gridPadding);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(col1PercentWidth);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(col2PercentWidth);
        gridPane.getColumnConstraints().addAll(col1, col2);
        return gridPane;
    }
}
