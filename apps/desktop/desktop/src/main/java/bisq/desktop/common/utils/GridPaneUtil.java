package bisq.desktop.common.utils;

import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.WrappingText;
import bisq.i18n.Res;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GridPaneUtil {
    /**
     * Set the grid pane with two column constraints.
     */
    public static void setGridPaneTwoColumnsConstraints(GridPane pane) {
        setGridPaneMultiColumnsConstraints(pane, 2);
    }

    /**
     * Set the grid pane with multiple column constraints.
     */
    public static void setGridPaneMultiColumnsConstraints(GridPane pane, int numColumns) {
        setGridPaneMultiColumnsConstraints(pane, numColumns, 100d / numColumns);
    }

    /**
     * Set the grid pane with multiple column constraints and custom percent width.
     */
    public static void setGridPaneMultiColumnsConstraints(GridPane pane, int numColumns, double percentWidth) {
        for (int i = 0; i < numColumns; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(percentWidth);
            pane.getColumnConstraints().add(col);
        }
    }

    /**
     * Get a custom two column grid pane with the layout and column widths specified.
     */
    public static GridPane getTwoColumnsGridPane(int horizontalGap,
                                                 int verticalGap,
                                                 Insets gridPadding) {
        GridPane gridPane = getGridPane(horizontalGap, verticalGap, gridPadding);
        setGridPaneTwoColumnsConstraints(gridPane);
        return gridPane;
    }

    /**
     * Get a GridPane with the horizontal and vertical gaps configured and paddings.
     */
    public static GridPane getGridPane(int horizontalGap, int verticalGap, Insets gridPadding) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(horizontalGap);
        gridPane.setVgap(verticalGap);
        gridPane.setPadding(gridPadding);
        return gridPane;
    }

    /**
     * Get icon and text as a label. See example at Dashboard, multiple trade protocols box.
     */
    public static HBox getIconAndText(String labelStyleClass, String text, String imageId) {
        Label label = new Label(text);
        label.getStyleClass().add(labelStyleClass);
        label.setWrapText(true);
        ImageView bulletPoint = ImageUtil.getImageViewById(imageId);
        HBox.setMargin(bulletPoint, new Insets(-3, 0, 0, 4));
        HBox horizontalBox = new HBox(15, bulletPoint, label);
        horizontalBox.setAlignment(Pos.CENTER_LEFT);
        return horizontalBox;
    }

    /**
     * Get info as a Text wrapped on a TextFlow. This allows for correct wrapping of the text.
     */
    public static TextFlow getInfoLabel(String info, String infoLabelStyleClass) {
        Text infoLabelText = new Text(info);
        infoLabelText.getStyleClass().add(infoLabelStyleClass);
        WrappingText subHeadline = new WrappingText(Res.get("dashboard.protocols.subHeadline"),"trade-protocols-overview-sub-headline");

        return new TextFlow(infoLabelText);
    }

    /**
     * Get headline label. If no icon is passed, outputs a simple headline.
     */
    public static Label getHeadline(String headline,
                                    String headlineStyleClass,
                                    String headlineImageId,
                                    double graphicTextGap) {
        Label headlineLabel;
        if (StringUtils.isNotEmpty(headlineImageId)) {
            headlineLabel = new Label(headline, ImageUtil.getImageViewById(headlineImageId));
            headlineLabel.setGraphicTextGap(graphicTextGap);
        } else {
            headlineLabel = new Label(headline);
        }
        headlineLabel.getStyleClass().add(headlineStyleClass);
        headlineLabel.setWrapText(true);
        return headlineLabel;
    }

    /**
     * Column box using a custom style. See example at Bisq Easy, best for beginners section.
     */
    public static void fillColumn(GridPane gridPane,
                                  int columnIndex,
                                  Button button,
                                  String buttonStyleClass,
                                  Insets buttonMargin,
                                  String headline,
                                  String headlineStyleClass,
                                  String headlineImageId,
                                  double headlineImageIdGap,
                                  Insets headlineMargin,
                                  String info,
                                  String infoLabelStyleClass,
                                  Insets infoMargin,
                                  double infoLineSpace,
                                  String groupPaneStyleClass,
                                  Insets groupPadding) {

        int gridPaneRows = 3;

        int rowCount = gridPane.getRowCount();
        //Reposition to previous count if this is a "right side" of a column
        if (columnIndex == 1) {
            rowCount = rowCount - gridPaneRows;
        }

        Pane group = new Pane();
        group.getStyleClass().add(groupPaneStyleClass);
        group.setPadding(groupPadding);

        gridPane.add(group, columnIndex, rowCount, 1, gridPaneRows);

        Label headlineLabel = getHeadline(headline,
                headlineStyleClass,
                headlineImageId,
                headlineImageIdGap);
        GridPane.setMargin(headlineLabel, headlineMargin);
        gridPane.add(headlineLabel, columnIndex, rowCount);

        TextFlow infoLabel = getInfoLabel(info, infoLabelStyleClass);
        infoLabel.setLineSpacing(infoLineSpace);
        GridPane.setMargin(infoLabel, infoMargin);
        gridPane.add(infoLabel, columnIndex, rowCount + 1);

        button.getStyleClass().add(buttonStyleClass);
        button.setMaxWidth(Double.MAX_VALUE);
        GridPane.setMargin(button, buttonMargin);
        gridPane.add(button, columnIndex, rowCount + 2);
    }

    /**
     * Add text field to a grid pane with a bind. Field text will show grayed out on textfield.
     */
    public static TextField addTextField(String labelText,
                                         StringProperty textFieldText,
                                         GridPane gridPane) {
        TextField textField = addTextField(labelText, textFieldText.get(), gridPane);
        textField.textProperty().bindBidirectional(textFieldText);
        return textField;
    }

    /**
     * Add text field to the grid pane.
     */
    public static TextField addTextField(String labelText, GridPane gridPane) {
        return addTextField(labelText, "", gridPane);
    }

    /**
     * Add text field to a grid pane. Field text will show grayed out on textfield when empty.
     */
    public static TextField addTextField(String labelText,
                                         String textFieldText,
                                         GridPane gridPane) {
        TextField textField = new TextField(textFieldText);
        textField.setPromptText(labelText);
        GridPane.setRowIndex(textField, gridPane.getRowCount());
        GridPane.setColumnIndex(textField, 0);
        GridPane.setColumnSpan(textField, gridPane.getColumnCount() + 1);
        //GridPane.setMargin(textField, new Insets(0, 0, 15, 0));
        gridPane.getChildren().addAll(textField);
        return textField;
    }
}
