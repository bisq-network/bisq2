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

package bisq.desktop.common.utils;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class KeyHandlerUtil {

    public static void handleShutDownKeyEvent(KeyEvent keyEvent, Runnable handler) {
        if (KeyCodeUtils.isCtrlPressed(KeyCode.W, keyEvent) ||
                KeyCodeUtils.isCtrlPressed(KeyCode.Q, keyEvent)) {
            handler.run();
        }
    }

    public static void handleEscapeKeyEvent(KeyEvent keyEvent, Runnable handler) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            keyEvent.consume();
            handler.run();
        }
    }

    public static void handleEnterKeyEvent(KeyEvent keyEvent, Runnable handler) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            keyEvent.consume();
            handler.run();
        }
    }
}