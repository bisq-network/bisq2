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

package bisq.tools.theme.moderna;

import bisq.tools.theme.ThemeDemoApp;
import lombok.extern.slf4j.Slf4j;

/**
 * Show case for basic JavaFx components used in Bisq with default JavaFx (moderna) styling
 * SamplePage is borrowed from: https://github.com/JFXtras/jfxtras-styles/
 */
@Slf4j
public class ModernaThemeDemoApp extends ThemeDemoApp {
    @Override
    protected void loadStyles() {
        // do nothing as we use defaults
    }

    @Override
    protected String getTitle() {
        return "Moderna";
    }
}
