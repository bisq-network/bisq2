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

package bisq.desktop;

import javafx.scene.Scene;

import static java.util.Objects.requireNonNull;

public class CssConfig {
    public static void addAllCss(Scene scene) {
        Class<? extends CssConfig> aClass = CssConfig.class;
        scene.getStylesheets().setAll(
                requireNonNull(aClass.getResource("/css/base.css")).toExternalForm(),
                requireNonNull(aClass.getResource("/css/text.css")).toExternalForm(),
                requireNonNull(aClass.getResource("/css/controls.css")).toExternalForm(),
                requireNonNull(aClass.getResource("/css/containers.css")).toExternalForm(),
                requireNonNull(aClass.getResource("/css/application.css")).toExternalForm(),
                requireNonNull(aClass.getResource("/css/chat.css")).toExternalForm(),
                requireNonNull(aClass.getResource("/css/user.css")).toExternalForm(),
                requireNonNull(aClass.getResource("/css/bisq_easy.css")).toExternalForm(),
                requireNonNull(aClass.getResource("/css/trade_apps.css")).toExternalForm(),
                requireNonNull(aClass.getResource("/css/images.css")).toExternalForm());
    }
}