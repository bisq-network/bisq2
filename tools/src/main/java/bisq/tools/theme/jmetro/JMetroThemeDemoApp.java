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

package bisq.tools.theme.jmetro;

import bisq.tools.theme.ThemeDemoApp;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.requireNonNull;

// Just temp as example for using an external theme
// Requires a gradle build for first run so that resource is in deployment directory
@Slf4j
public class JMetroThemeDemoApp  extends ThemeDemoApp {
    @Override
    protected void loadStyles() {
        scene.getStylesheets().setAll(
                requireNonNull(getClass().getResource("base.css")).toExternalForm(),
                requireNonNull(getClass().getResource("base_extras.css")).toExternalForm(),
                requireNonNull(getClass().getResource("base_other_libraries.css")).toExternalForm(),
                requireNonNull(getClass().getResource("dark_theme.css")).toExternalForm(),
                requireNonNull(getClass().getResource("light_theme.css")).toExternalForm());
    }

    @Override
    protected String getTitle() {
        return "JMetro";
    }
}
