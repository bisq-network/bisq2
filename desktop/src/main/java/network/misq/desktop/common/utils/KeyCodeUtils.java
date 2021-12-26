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

package network.misq.desktop.common.utils;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class KeyCodeUtils {
    public static boolean isAltOrCtrlPressed(KeyCode keyCode, KeyEvent keyEvent) {
        return isAltPressed(keyCode, keyEvent) || isCtrlPressed(keyCode, keyEvent);
    }

    public static boolean isCtrlPressed(KeyCode keyCode, KeyEvent keyEvent) {
        return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN).match(keyEvent) ||
                new KeyCodeCombination(keyCode, KeyCombination.CONTROL_DOWN).match(keyEvent);
    }

    public static boolean isAltPressed(KeyCode keyCode, KeyEvent keyEvent) {
        return new KeyCodeCombination(keyCode, KeyCombination.ALT_DOWN).match(keyEvent);
    }

    public static boolean isCtrlShiftPressed(KeyCode keyCode, KeyEvent keyEvent) {
        return new KeyCodeCombination(keyCode, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(keyEvent);
    }
}
