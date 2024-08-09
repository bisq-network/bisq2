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

package bisq.webcam.view.util;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Objects;

@Slf4j
public class ImageUtil {
    // Does not resolve the @2x automatically
    public static Image getImageByPath(String path) {
        try (InputStream resourceAsStream = ImageUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (resourceAsStream == null) {
                return null;
            }
            return new Image(Objects.requireNonNull(resourceAsStream));
        } catch (Exception e) {
            log.error("Loading image failed: path={}", path);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void addAppIcons(Stage stage) {
        stage.getIcons().add(ImageUtil.getImageByPath("images/app_window/icon_512.png"));
        stage.getIcons().add(ImageUtil.getImageByPath("images/app_window/icon_256.png"));
        stage.getIcons().add(ImageUtil.getImageByPath("images/app_window/icon_128.png"));
        stage.getIcons().add(ImageUtil.getImageByPath("images/app_window/icon_64.png"));
        stage.getIcons().add(ImageUtil.getImageByPath("images/app_window/icon_32.png"));
        stage.getIcons().add(ImageUtil.getImageByPath("images/app_window/icon_16.png"));
    }
}
