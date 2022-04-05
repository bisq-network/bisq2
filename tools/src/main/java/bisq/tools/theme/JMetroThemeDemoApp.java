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

import static java.util.Objects.requireNonNull;

// Just temp as example for using an external theme
// Requires a gradle build for first run so that resource is in deployment directory
@Slf4j
public class JMetroThemeDemoApp extends Application {
    @Override
    public void start(Stage stage) {
        ScrollPane scrollPane = new ScrollPane();
        SamplePage samplePage = new SamplePage();
        scrollPane.setContent(samplePage);
        Scene scene = new Scene(scrollPane, 1400, 1000);
        stage.setTitle(getClass().getSimpleName());
        stage.setScene(scene);

        // adding the theme in setUserAgentStylesheet cause an error...
        // scene.setUserAgentStylesheet(requireNonNull(getClass().getResource("jmetro/base.css")).toExternalForm());
        scene.getStylesheets().setAll(
                requireNonNull(getClass().getResource("jmetro/base.css")).toExternalForm(),
                requireNonNull(getClass().getResource("jmetro/base_extras.css")).toExternalForm(),
                requireNonNull(getClass().getResource("jmetro/base_other_libraries.css")).toExternalForm(),
                requireNonNull(getClass().getResource("jmetro/dark_theme.css")).toExternalForm(),
                requireNonNull(getClass().getResource("jmetro/light_theme.css")).toExternalForm());


        stage.show();
    }
}
