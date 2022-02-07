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

package bisq.desktop.overlay.window;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqLabel;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PopupWindowView extends View<VBox, PopupWindowModel, PopupWindowController> {

    private GridPane gridPane;
    protected double width = DEFAULT_WIDTH;
    private BisqLabel headLineLabel, messageLabel;
    private Label headlineIcon;
    private String headLine, headlineStyle, message, truncatedMessage;
    private List<String> messageHyperlinks;
    protected double buttonDistance = 20;
    protected Type type = Type.Undefined;

    public PopupWindowView(PopupWindowModel model, PopupWindowController controller) {
        super(new VBox(), model, controller);
        root.setPadding(new Insets(20, 20, 20, 0));
        // root.getChildren().add(new Label(getClass().getSimpleName()));


        headLine = "test headline";
        message = "test message";
        truncatedMessage = message;

        createGridPane();
        addHeadLine();
        addMessage();
        addFooter();
    }

    @Override
    public void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    protected final static double DEFAULT_WIDTH = 668;


    private enum AnimationType {
        FadeInAtCenter,
        SlideDownFromCenterTop,
        SlideFromRightTop,
        ScaleDownToCenter,
        ScaleFromCenter,
        ScaleYFromCenter
    }

    private enum ChangeBackgroundType {
        BlurLight,
        BlurUltraLight,
        Darken
    }

    protected enum Type {
        Undefined(AnimationType.ScaleFromCenter, ChangeBackgroundType.BlurLight),

        Notification(AnimationType.SlideFromRightTop, ChangeBackgroundType.BlurLight),

        BackgroundInfo(AnimationType.SlideDownFromCenterTop, ChangeBackgroundType.BlurUltraLight),
        Feedback(AnimationType.SlideDownFromCenterTop, ChangeBackgroundType.Darken),

        Information(AnimationType.FadeInAtCenter, ChangeBackgroundType.BlurLight),
        Instruction(AnimationType.ScaleFromCenter, ChangeBackgroundType.BlurLight),
        Attention(AnimationType.ScaleFromCenter, ChangeBackgroundType.BlurLight),
        Confirmation(AnimationType.ScaleYFromCenter, ChangeBackgroundType.BlurLight),

        Warning(AnimationType.ScaleDownToCenter, ChangeBackgroundType.BlurLight),
        Error(AnimationType.ScaleDownToCenter, ChangeBackgroundType.BlurLight);

        public final AnimationType animationType;
        public final ChangeBackgroundType changeBackgroundType;

        Type(AnimationType animationType, ChangeBackgroundType changeBackgroundType) {
            this.animationType = animationType;
            this.changeBackgroundType = changeBackgroundType;
        }
    }



   /* public OverlayView(OverlayModel model, OverlayController controller, Scene parentScene) {
        super(new AnchorPane(), model, controller);
        this.model = model;
        this.controller = controller;
        this.parentScene = parentScene;

        Scene scene = new Scene(root);
        scene.getStylesheets().setAll(parentScene.getStylesheets());
        scene.setFill(Color.TRANSPARENT);
        stage = new Stage();
        stage.setScene(scene);
        Window parentWindow = parentScene.getWindow();
        stage.initOwner(parentWindow);
        setModality();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.sizeToScene();
        stage.setOnCloseRequest(event -> {
            event.consume();
            controller.onClosed();
        });
        setupKeyHandler(scene);

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                getRoot().getChildren().setAll(newValue.getRoot());
                stage.show();
            } else {
                getRoot().getChildren().clear();
                stage.hide();
            }
        });

        headLine = "test headline";
        message = "test message";
        truncatedMessage = message;

        createGridPane();
        addHeadLine();
        addMessage();
        addFooter();
        applyStyles();
    }*/

    protected void createGridPane() {
        gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(64, 64, 64, 64));
        gridPane.setPrefWidth(width);

        ColumnConstraints columnConstraints1 = new ColumnConstraints();
        columnConstraints1.setHalignment(HPos.RIGHT);
        columnConstraints1.setHgrow(Priority.SOMETIMES);
        ColumnConstraints columnConstraints2 = new ColumnConstraints();
        columnConstraints2.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(columnConstraints1, columnConstraints2);
        root.getChildren().add(gridPane);
    }


    protected void addHeadLine() {
        if (headLine != null) {
            // ++rowIndex;

            HBox hBox = new HBox();
            hBox.setSpacing(7);
            headLineLabel = new BisqLabel(headLine);
            headlineIcon = new Label();
            headlineIcon.setManaged(false);
            headlineIcon.setVisible(false);
            headlineIcon.setPadding(new Insets(3));
            headLineLabel.setMouseTransparent(true);

            if (headlineStyle != null)
                headLineLabel.setStyle(headlineStyle);

            hBox.getChildren().addAll(headlineIcon, headLineLabel);

            GridPane.setHalignment(hBox, HPos.LEFT);
            GridPane.setRowIndex(hBox, gridPane.getRowCount());
            GridPane.setColumnSpan(hBox, 2);
            gridPane.getChildren().addAll(hBox);
        }
    }

    protected void addMessage() {
        if (message != null) {
            messageLabel = new BisqLabel(truncatedMessage);
            messageLabel.setMouseTransparent(true);
            messageLabel.setWrapText(true);
            GridPane.setHalignment(messageLabel, HPos.LEFT);
            GridPane.setHgrow(messageLabel, Priority.ALWAYS);
            GridPane.setMargin(messageLabel, new Insets(3, 0, 0, 0));
            GridPane.setRowIndex(messageLabel, gridPane.getRowCount());
            GridPane.setColumnIndex(messageLabel, 0);
            GridPane.setColumnSpan(messageLabel, 2);
            gridPane.getChildren().add(messageLabel);
            addFooter();
        }
    }

    private void addFooter() {
        if (messageHyperlinks != null && messageHyperlinks.size() > 0) {
            VBox footerBox = new VBox();
            GridPane.setRowIndex(footerBox, gridPane.getRowCount());
            GridPane.setColumnSpan(footerBox, 2);
            GridPane.setMargin(footerBox, new Insets(buttonDistance, 0, 0, 0));
            gridPane.getChildren().add(footerBox);
            for (int i = 0; i < messageHyperlinks.size(); i++) {
                Label label = new Label(String.format("[%d]", i + 1));
                Hyperlink link = new Hyperlink(messageHyperlinks.get(i));
                //todo
                // link.setOnAction(event -> GUIUtil.openWebPageNoPopup(link.getText()));
                footerBox.getChildren().addAll(new HBox(label, link));
            }
        }
    }

  /*  private void setModality() {
        stage.initModality(Modality.WINDOW_MODAL);
    }

    private void setupKeyHandler(Scene scene) {

    }*/

    public void display() {
        // layout();
        //addEffectToBackground();

        // On Linux the owner stage does not move the child stage as it does on Mac
        // So we need to apply centerPopup. Further with fast movements the handler loses
        // the latest position, with a delay it fixes that.
        // Also on Mac sometimes the popups are positioned outside of the main app, so keep it for all OS
 /*       positionListener = (observable, oldValue, newValue) -> {
            if (stage != null) {
                layout();
                if (centerTime != null)
                    centerTime.stop();

                centerTime = UserThread.runAfter(this::layout, 3);
            }
        };
        window.xProperty().addListener(positionListener);
        window.yProperty().addListener(positionListener);
        window.widthProperty().addListener(positionListener);

        animateDisplay();
        isDisplayed = true;*/
    }
}
