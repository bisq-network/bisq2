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

package bisq.desktop.primary;

import bisq.common.application.DevMode;
import bisq.common.util.OsUtils;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.KeyCodeUtils;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.NavigationView;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.requireNonNull;

@Slf4j
public class PrimaryStageView extends NavigationView<AnchorPane, PrimaryStageModel, PrimaryStageController> {
    private final Stage stage;
    @Getter
    private final Scene scene;

    public PrimaryStageView(PrimaryStageModel model, PrimaryStageController controller, Stage stage) {
        super(new AnchorPane(), model, controller);

        this.stage = stage;

        root.getStyleClass().add("bisq-content-bg");

        scene = new Scene(root); // takes about  50 ms
        try {
            stage.setTitle(model.getTitle());
            stage.getIcons().add(getApplicationIconImage());

            configCss();
            configSizeAndPosition();
            configKeyEventHandlers();

            stage.setOnCloseRequest(event -> {
                event.consume();
                controller.onQuit();
            });


            model.getView().addListener((observable, oldValue, newValue) -> {
                Layout.pinToAnchorPane(newValue.getRoot(), 0, 0, 0, 0);
                root.getChildren().add(newValue.getRoot());
                if (oldValue != null) {
                    Transitions.fadeOutAndRemove(oldValue.getRoot());
                    Transitions.fadeIn(newValue.getRoot());
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
            controller.onQuit();
        }
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    void showStage() {
        long ts = System.currentTimeMillis();
        stage.show();
        stage.setScene(scene); // Here we trigger onViewAttached in controller
        log.info("Attaching view to stage took {} ms", System.currentTimeMillis() - ts);
    }

    private void configCss() {
        scene.getStylesheets().setAll(
                requireNonNull(getClass().getResource("/bisq.css")).toExternalForm(),
                requireNonNull(getClass().getResource("/images.css")).toExternalForm());
    }

    private void configSizeAndPosition() {
        root.setPrefWidth(model.getPrefWidth());
        root.setPrefHeight(model.getPrefHeight());
        model.getStageX().ifPresent(stage::setX);
        model.getStageY().ifPresent(stage::setY);
        model.getStageWidth().ifPresent(stage::setWidth);
        model.getStageHeight().ifPresent(stage::setHeight);
        stage.setMinWidth(model.getMinWidth());
        stage.setMinHeight(model.getMinHeight());
        stage.xProperty().addListener((observable, oldValue, newValue) -> controller.onStageXChanged((double) newValue));
        stage.yProperty().addListener((observable, oldValue, newValue) -> controller.onStageYChanged((double) newValue));
        stage.widthProperty().addListener((observable, oldValue, newValue) -> controller.onStageWidthChanged((double) newValue));
        stage.heightProperty().addListener((observable, oldValue, newValue) -> controller.onStageHeightChanged((double) newValue));
    }

    private void configKeyEventHandlers() {
        scene.setOnKeyPressed(keyEvent -> {
            if (KeyCodeUtils.isCtrlPressed(KeyCode.W, keyEvent) ||
                    KeyCodeUtils.isCtrlPressed(KeyCode.Q, keyEvent)) {
                controller.onQuit();
            }
            if (DevMode.isDevMode()) {
                if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT1, keyEvent)) {
                    Navigation.navigateTo(NavigationTarget.SPLASH);
                } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT2, keyEvent)) {
                    Navigation.navigateTo(NavigationTarget.BISQ_EASY_ONBOARDING);
                } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT3, keyEvent)) {
                    Navigation.navigateTo(NavigationTarget.ONBOARDING_DIRECTION);
                } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT4, keyEvent)) {
                    // Navigation.navigateTo(NavigationTarget.ONBOARD_NEWBIE);
                } else if (KeyCodeUtils.isCtrlPressed(KeyCode.DIGIT5, keyEvent)) {
                    //  Navigation.navigateTo(NavigationTarget.ONBOARD_PRO_TRADER);
                }
            }
        });
    }

    private Image getApplicationIconImage() {
        String iconPath;
        if (OsUtils.isOSX())
            iconPath = ImageUtil.isRetina() ? "images/window_icon@2x.png" : "images/window_icon.png";
        else if (OsUtils.isWindows())
            iconPath = "images/task_bar_icon_windows.png";
        else
            iconPath = "images/task_bar_icon_linux.png";

        return ImageUtil.getImageByPath(iconPath);
    }
}
