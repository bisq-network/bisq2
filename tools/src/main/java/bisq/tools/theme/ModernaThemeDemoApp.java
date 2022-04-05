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

import bisq.tools.theme.sample.SamplePage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * Show case for basic JavaFx components used in Bisq with default JavaFx (moderna) styling
 * SamplePage is borrowed from: https://github.com/JFXtras/jfxtras-styles/
 */
@Slf4j
public class ModernaThemeDemoApp extends Application {
    @Override
    public void start(Stage stage) {
        ScrollPane scrollPane = new ScrollPane();
        SamplePage samplePage = new SamplePage();
        scrollPane.setContent(samplePage);
        Scene scene = new Scene(scrollPane, 1400, 1000);
        stage.setTitle(getClass().getSimpleName());
        stage.setScene(scene);

        stage.show();
    }
}
