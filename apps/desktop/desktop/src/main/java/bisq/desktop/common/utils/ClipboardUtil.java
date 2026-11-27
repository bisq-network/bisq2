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

import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ClipboardUtil {
    public static void copyToClipboard(String content) {
        if (content != null && !content.isEmpty()) {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(content);
            setClipboardContent(clipboardContent);
        }
    }

    public static void copyToClipboard(Image image) {
        if (image != null) {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putImage(image);
            setClipboardContent(clipboardContent);
        }
    }

    public static Optional<String> getClipboardString() {
        try {
            return Optional.of(Clipboard.getSystemClipboard().getString());
        } catch (Throwable e) {
            log.error("getClipboardString failed ", e);
            return Optional.empty();
        }
    }

    private static void setClipboardContent(ClipboardContent content) {
        try {
            Clipboard.getSystemClipboard().setContent(content);
        } catch (Exception e) {
            log.error("Failed to set clipboard content", e);
        }
    }
}
