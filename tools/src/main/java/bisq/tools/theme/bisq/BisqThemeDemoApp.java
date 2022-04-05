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

package bisq.tools.theme.bisq;

import bisq.tools.theme.ThemeDemoApp;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.requireNonNull;

/**
 * Show case for basic JavaFx components used in Bisq with custom styling for Bisq
 * SamplePage is borrowed from: https://github.com/JFXtras/jfxtras-styles/
 */
// Requires a gradle build for first run so that resource is in deployment directory
@Slf4j
public class BisqThemeDemoApp extends ThemeDemoApp {
    @Override
    protected void loadStyles() {
        scene.setUserAgentStylesheet(requireNonNull(getClass().getResource("bisq.css")).toExternalForm());
        scene.getStylesheets().setAll(requireNonNull(getClass().getResource("dark.css")).toExternalForm());
    }

    @Override
    protected String getTitle() {
        return "Bisq";
    }
}

