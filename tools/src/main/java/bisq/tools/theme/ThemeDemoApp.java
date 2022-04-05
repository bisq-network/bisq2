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

package bisq.tools.theme;

import bisq.desktop.common.utils.KeyCodeUtils;
import bisq.tools.theme.sample.SamplePage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

// Just temp as example for using an external theme
// Requires a gradle build for first run so that resource is in deployment directory
@Slf4j
public abstract class ThemeDemoApp extends Application {
    protected Scene scene;

    @Override
    public void start(Stage stage) {
        ScrollPane scrollPane = new ScrollPane();
        SamplePage samplePage = new SamplePage();
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

    protected abstract void loadStyles();

    protected abstract String getTitle();
}
