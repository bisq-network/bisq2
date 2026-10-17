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

package bisq.desktop_app;

import bisq.application.migration.MigrationListener;
import bisq.common.platform.OS;
import bisq.common.platform.Version;
import bisq.desktop.CssConfig;
import bisq.desktop.DesktopModel;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.application.JavaFxApplicationData;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * Shows the migrations of the data directory in the main window, before the desktop UI exists. It looks like the
 * user agreement: a panel over the darkened splash logo. While the migrations run, the panel shows a progress bar. If a
 * migration failed, it warns the user and lets them shut down, the default, or start anyway.
 * <p>
 * The main window gets the default size, as the saved size is only read after the migrations. DesktopView later
 * replaces the scene and applies the saved size and position.
 */
@Slf4j
class MigrationScreen implements MigrationListener {
    private static final double PADDING = 30;

    private final CompletableFuture<JavaFxApplicationData> onApplicationLaunched;
    // Only accessed on the JavaFX thread
    @Nullable
    private VBox panel;

    MigrationScreen(CompletableFuture<JavaFxApplicationData> onApplicationLaunched) {
        this.onApplicationLaunched = onApplicationLaunched;
    }

    @Override
    public void onMigrationsStarted() {
        runOnJavaFxThreadAndWait(stage -> show(stage,
                Res.get("popup.migration.headline"),
                Res.get("popup.migration.inProgress"),
                createProgressBar()));
    }

    // The panel shows until DesktopView replaces the scene, after the services were created and the data was read
    @Override
    public void onMigrationsCompleted() {
        runOnJavaFxThreadAndWait(stage -> showStarting(stage, Res.get("popup.migration.completed.headline")));
    }

    // Shutting down is the default action, as starting on a data directory which is not fully migrated can damage data
    @Override
    public boolean startAfterFailedMigration(Version failedVersion, Path appDataDirPath) {
        CompletableFuture<Boolean> startAnyway = new CompletableFuture<>();
        boolean shown = runOnJavaFxThreadAndWait(stage -> {
            Button startAnywayButton = new Button(Res.get("popup.migration.failed.startAnyway"));
            startAnywayButton.getStyleClass().add("outlined-button");
            startAnywayButton.setOnAction(event -> {
                showStarting(stage, Res.get("popup.migration.starting.headline"));
                startAnyway.complete(true);
            });
            Button shutDownButton = new Button(Res.get("action.shutDown"));
            // Enter and Escape both shut down
            shutDownButton.setDefaultButton(true);
            shutDownButton.setCancelButton(true);
            shutDownButton.setOnAction(event -> startAnyway.complete(false));
            HBox buttons = new HBox(20, startAnywayButton, shutDownButton);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            show(stage,
                    Res.get("popup.migration.failed.headline"),
                    Res.get("popup.migration.failed.msg", appDataDirPath),
                    buttons);
            // While the user decides, closing the window means shutting down
            stage.setOnCloseRequest(event -> {
                event.consume();
                startAnyway.complete(false);
            });
            shutDownButton.requestFocus();
        });
        // Without a window, nobody can choose to start anyway
        return shown && startAnyway.join();
    }

    private void showStarting(Stage stage, String headline) {
        show(stage, headline, Res.get("popup.migration.starting"), createProgressBar());
        // Closing the window would not stop the start. DesktopView replaces this handler.
        stage.setOnCloseRequest(Event::consume);
    }

    private static ProgressBar createProgressBar() {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setMinHeight(3);
        progressBar.setMaxHeight(3);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(progressBar, new Insets(10, 0, 10, 0));
        return progressBar;
    }

    private void show(Stage stage, String headline, String message, Node bottom) {
        if (panel == null) {
            panel = showMainWindow(stage);
        }

        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().add("tac-headline");
        headlineLabel.setWrapText(true);
        headlineLabel.setAlignment(Pos.CENTER);
        headlineLabel.setMaxWidth(Double.MAX_VALUE);

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("bisq-text-3");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);

        panel.getChildren().setAll(headlineLabel, messageLabel, bottom);
        stage.setTitle(headline);
    }

    private static VBox showMainWindow(Stage stage) {
        ImageView logo = new ImageView();
        logo.setId("logo-splash");
        StackPane background = new StackPane(logo);
        background.getStyleClass().add("bisq-content-bg");
        // Like the main window behind an overlay
        background.setEffect(new ColorAdjust(0, 0, Transitions.VERY_DARK_BRIGHTNESS, 0));

        VBox panel = new VBox(20);
        panel.getStyleClass().add("popup-bg");
        panel.setPadding(new Insets(PADDING));
        panel.setPrefWidth(OverlayModel.WIDTH);
        panel.setMaxWidth(OverlayModel.WIDTH);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane root = new StackPane(background, panel);
        Scene scene = new Scene(root);
        CssConfig.addAllCss(scene);

        // The same size and position as DesktopController uses when no size is saved
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        double width = DesktopModel.defaultStageWidth(screenBounds);
        double height = DesktopModel.defaultStageHeight(screenBounds);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX((screenBounds.getWidth() - width) / 2);
        stage.setY((screenBounds.getHeight() - height) / 2);
        if (!OS.isMacOs()) {
            // DesktopView does not show the icons on macOS either
            ImageUtil.addAppIcons(stage);
        }
        // Closing the window would not stop the migrations. DesktopView replaces this handler.
        stage.setOnCloseRequest(Event::consume);
        stage.setScene(scene);
        stage.show();
        return panel;
    }

    // Returns false if JavaFX did not start or the action failed. The migrations then continue without a window, and the
    // start does not wait for a choice which nobody can make.
    private boolean runOnJavaFxThreadAndWait(Consumer<Stage> action) {
        try {
            Stage stage = onApplicationLaunched.join().getStage();
            CompletableFuture<Void> done = new CompletableFuture<>();
            Platform.runLater(() -> {
                try {
                    action.accept(stage);
                    done.complete(null);
                } catch (Throwable t) {
                    done.completeExceptionally(t);
                }
            });
            done.join();
            return true;
        } catch (CompletionException e) {
            log.warn("Could not show the migration screen", e);
            return false;
        }
    }
}
