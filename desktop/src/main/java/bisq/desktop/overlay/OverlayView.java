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
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OverlayView extends View<AnchorPane, OverlayModel, OverlayController> {
    private final Scene parentScene;
    private final OverlayModel model;
    private final OverlayController controller;
    private final Stage stage;

    public OverlayView(OverlayModel model, OverlayController controller, Scene parentScene) {
        super(new AnchorPane(), model, controller);
        this.model = model;
        this.controller = controller;
        this.parentScene = parentScene;

        Scene scene = new Scene(getRoot());
        scene.getStylesheets().setAll(parentScene.getStylesheets());
        //scene.setFill(Color.TRANSPARENT);
        stage = new Stage();
        stage.setScene(scene);
        Window parentWindow = parentScene.getWindow();
        stage.initOwner(parentWindow);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setOnCloseRequest(event -> {
            event.consume();
            controller.onClosed();
        });
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
    
  /*  protected void layout() {
        if (owner == null)
            owner = MainView.getRootContainer();
        Scene rootScene = owner.getScene();
        if (rootScene != null) {
            Window window = rootScene.getWindow();
            double titleBarHeight = window.getHeight() - rootScene.getHeight();
            if (Utilities.isWindows())
                titleBarHeight -= 9;
            stage.setX(Math.round(window.getX() + (owner.getWidth() - stage.getWidth()) / 2));

            if (type.animationType == AnimationType.SlideDownFromCenterTop)
                stage.setY(Math.round(window.getY() + titleBarHeight));
            else
                stage.setY(Math.round(window.getY() + titleBarHeight + (owner.getHeight() - stage.getHeight()) / 2));
        }
    }*/

}
