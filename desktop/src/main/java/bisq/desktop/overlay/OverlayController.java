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

import bisq.application.DefaultApplicationService;
import bisq.common.util.OsUtils;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.newProfilePopup.NewProfilePopupController;
import bisq.settings.DisplaySettings;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Popup for usage for views using the MVC pattern.
 */
@Slf4j
public class OverlayController extends NavigationController {

    private static final Interpolator INTERPOLATOR = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);

    private static Region owner;
    private static DisplaySettings displaySettings;
    @Getter
    private final OverlayModel model;
    @Getter
    private final OverlayView view;
    private final DefaultApplicationService applicationService;

    public static void init(Region owner, DisplaySettings displaySettings) {
        OverlayController.owner = owner;
        OverlayController.displaySettings = displaySettings;
    }

    @Getter
    protected Pane root;
    protected final Pane base = new Pane();
    protected final Scene ownerScene;
    protected final Stage stage;
    protected final Window window;

    protected ChangeListener<Number> positionListener;
    protected UIScheduler centerTime;

    public OverlayController(DefaultApplicationService applicationService) {
        super(NavigationTarget.OVERLAY);

        this.applicationService = applicationService;

        model = new OverlayModel();
        view = new OverlayView(model, this);

        // root = new VBox();
        base.getStyleClass().add("popup-bg");

        ownerScene = owner.getScene();

        Scene scene = new Scene(base);
        scene.getStylesheets().setAll(ownerScene.getStylesheets());
        scene.setFill(Color.TRANSPARENT);
        setupKeyHandler(scene);

        stage = new Stage();
        stage.setScene(scene);
        stage.sizeToScene();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(owner.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setOnCloseRequest(event -> {
            event.consume();
            hide();
        });
        window = ownerScene.getWindow();


        // On Linux the owner stage does not move the child stage as it does on Mac
        // So we need to apply centerPopup. Further, with fast movements the handler loses
        // the latest position, with a delay it fixes that.
        // Also, on Mac sometimes the popups are positioned outside the main app, so keep it for all OS
        positionListener = (observable, oldValue, newValue) -> {
            layout();

            if (centerTime != null)
                centerTime.stop();

            centerTime = UIScheduler.run(this::layout).after(3000);
        };
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //  Controller implementation
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onActivate() {
        log.error("onActivate");
    }

    @Override
    public void onDeactivate() {

    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case ONBOARDING2 -> {
                return Optional.of(new NewProfilePopupController(applicationService));
            }
            case OVERLAY_CLOSE -> {
                return Optional.empty();
            }

            default -> {
                return Optional.empty();
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void show() {
        root.setPrefWidth(owner.getWidth() - 180);
        root.setPrefHeight(owner.getHeight() - 100);

        window.xProperty().addListener(positionListener);
        window.yProperty().addListener(positionListener);
        window.widthProperty().addListener(positionListener);

        // base.getChildren().add(root);
        stage.show();

        layout();

        Transitions.darken(owner, Transitions.DEFAULT_DURATION, false);

        animateDisplay();
    }

    public void hide() {
        animateHide();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////

    protected void animateHide() {
        animateHide(() -> {
            Transitions.removeEffect(owner);

            if (stage != null) {
                stage.hide();
            }

            if (centerTime != null) {
                centerTime.stop();
            }

            Scene rootScene = owner.getScene();
            if (rootScene != null) {
                Window window = rootScene.getWindow();
                if (window != null && positionListener != null) {
                    window.xProperty().removeListener(positionListener);
                    window.yProperty().removeListener(positionListener);
                    window.widthProperty().removeListener(positionListener);
                }
            }

            base.getChildren().clear();
            resetSelectedChildTarget();
        });
    }

    protected void setupKeyHandler(Scene scene) {
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER) {
                e.consume();
                hide();
            }
        });
    }

    protected void animateDisplay() {
        root.setOpacity(0);
        double duration = getDuration(400);
        Timeline timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();

        double startScale = 0.25;
        keyFrames.add(new KeyFrame(Duration.millis(0),
                new KeyValue(root.opacityProperty(), 0, INTERPOLATOR),
                new KeyValue(root.scaleXProperty(), startScale, INTERPOLATOR),
                new KeyValue(root.scaleYProperty(), startScale, INTERPOLATOR)

        ));
        keyFrames.add(new KeyFrame(Duration.millis(duration),
                new KeyValue(root.opacityProperty(), 1, INTERPOLATOR),
                new KeyValue(root.scaleXProperty(), 1, INTERPOLATOR),
                new KeyValue(root.scaleYProperty(), 1, INTERPOLATOR)
        ));

        timeline.play();
    }

    protected void animateHide(Runnable onFinishedHandler) {
        double duration = getDuration(200);
        Timeline timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
        double endScale = 0.25;
        keyFrames.add(new KeyFrame(Duration.millis(0),
                new KeyValue(root.opacityProperty(), 1, INTERPOLATOR),
                new KeyValue(root.scaleXProperty(), 1, INTERPOLATOR),
                new KeyValue(root.scaleYProperty(), 1, INTERPOLATOR)
        ));
        keyFrames.add(new KeyFrame(Duration.millis(duration),
                new KeyValue(root.opacityProperty(), 0, INTERPOLATOR),
                new KeyValue(root.scaleXProperty(), endScale, INTERPOLATOR),
                new KeyValue(root.scaleYProperty(), endScale, INTERPOLATOR)
        ));

        timeline.setOnFinished(e -> onFinishedHandler.run());
        timeline.play();
    }

    protected void layout() {
        double titleBarHeight = window.getHeight() - ownerScene.getHeight();

        if (OsUtils.isWindows()) {
            titleBarHeight -= 9;
        }
        stage.setX(Math.round(window.getX() + (owner.getWidth() - stage.getWidth()) / 2));
        stage.setY(Math.round(window.getY() + titleBarHeight + (owner.getHeight() - stage.getHeight()) / 2));
    }

    protected double getDuration(double duration) {
        return displaySettings.isUseAnimations() ? duration : 1;
    }
}
