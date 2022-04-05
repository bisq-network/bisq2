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

package bisq.tools.theme.currentbisq;

import bisq.desktop.common.utils.KeyCodeUtils;
import bisq.i18n.Res;
import bisq.tools.theme.currentbisq.sample.CurrentBisqSamplePage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Show case for basic JavaFx components used in Bisq with custom styling for Bisq
 * SamplePage is borrowed from: https://github.com/JFXtras/jfxtras-styles/
 */
// Requires a gradle build for first run so that resource is in deployment directory
@Slf4j
public class CurrentBisqThemeDemoApp extends Application {
    private Scene scene;

    @Override
    public void start(Stage stage) {
        Res.initialize(Locale.US);
        ScrollPane scrollPane = new ScrollPane();
        CurrentBisqSamplePage samplePage = new CurrentBisqSamplePage();
        scrollPane.setContent(samplePage);
        scene = new Scene(scrollPane, 1400, 1000);
        stage.setTitle(getTitle());
        stage.setScene(scene);

        loadStyles();
        scene.addEventHandler(KeyEvent.KEY_RELEASED, keyEvent -> {
            if (KeyCodeUtils.isCtrlPressed(KeyCode.W, keyEvent) ||
                    KeyCodeUtils.isCtrlPressed(KeyCode.Q, keyEvent)) {
                Platform.exit();
            }
        });
        stage.show();
    }

    protected void loadStyles() {
        scene.getStylesheets().setAll(
                requireNonNull(getClass().getResource("current_bisq.css")).toExternalForm(),
                requireNonNull(getClass().getResource("current_bisq2.css")).toExternalForm(),
                requireNonNull(getClass().getResource("current_theme-dark.css")).toExternalForm()
        );
    }

    protected String getTitle() {
        return "Current Bisq";
    }
}

