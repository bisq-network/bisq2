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

package bisq.desktop.overlay;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqLabel;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class OverlayView extends View<AnchorPane, OverlayModel, OverlayController> {
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

    private final Scene parentScene;
    private final OverlayModel model;
    private final OverlayController controller;
    private final Stage stage;
    private GridPane gridPane;
    protected double width = DEFAULT_WIDTH;
    private BisqLabel headLineLabel, messageLabel;
    private Label headlineIcon;
    private String headLine, headlineStyle, message, truncatedMessage;
    private List<String> messageHyperlinks;
    protected double buttonDistance = 20;
    protected Type type = Type.Undefined;


    public OverlayView(OverlayModel model, OverlayController controller, Scene parentScene) {
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
        applyStyles();
        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                getRoot().getChildren().setAll(newValue.getRoot());
                stage.show();
            } else {
                getRoot().getChildren().clear();
                stage.hide();
            }
        });
    }


    private void setModality() {
        stage.initModality(Modality.WINDOW_MODAL);
    }

    private void setupKeyHandler(Scene scene) {

    }

    protected void applyStyles() {
        Parent rootContainer = parentScene.getRoot();
        if (type.animationType == AnimationType.SlideDownFromCenterTop) {
            rootContainer.getStyleClass().add("popup-bg-top");
        } else {
            rootContainer.getStyleClass().add("popup-bg");
        }


        if (headLineLabel != null) {

            switch (type) {
                case Information:
                case BackgroundInfo:
                case Instruction:
                case Confirmation:
                case Feedback:
                case Notification:
                case Attention:
                    headLineLabel.getStyleClass().add("popup-headline-information");
                    headlineIcon.getStyleClass().add("popup-icon-information");
                    headlineIcon.setManaged(true);
                    headlineIcon.setVisible(true);
                    // FormBuilder.getIconForLabel(AwesomeIcon.INFO_SIGN, headlineIcon, "1.5em");
                    break;
                case Warning:
                case Error:
                    headLineLabel.getStyleClass().add("popup-headline-warning");
                    headlineIcon.getStyleClass().add("popup-icon-warning");
                    headlineIcon.setManaged(true);
                    headlineIcon.setVisible(true);
                    //  FormBuilder.getIconForLabel(AwesomeIcon.EXCLAMATION_SIGN, headlineIcon, "1.5em");
                    break;
                default:
                    headLineLabel.getStyleClass().add("popup-headline");
            }
        }
    }
}
