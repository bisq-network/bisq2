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

import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Objects;

@Slf4j
public class ImageUtil {

    // Does not resolve the @2x automatically
    public static Image getImageByPath(String path) {
        try (InputStream resourceAsStream = ImageView.class.getClassLoader().getResourceAsStream(path)) {
            return new Image(Objects.requireNonNull(resourceAsStream));
        } catch (Exception e) {
            log.error("Loading image failed: path={}", path);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Image getImageByPath(String path, int width, int height) {
        try (InputStream resourceAsStream = ImageView.class.getClassLoader().getResourceAsStream(path)) {
            return new Image(Objects.requireNonNull(resourceAsStream), width, height, true, true);
        } catch (Exception e) {
            log.error("Loading image failed: path={}", path);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // Does resolve the @2x automatically
    public static ImageView getImageViewById(String id) {
        ImageView imageView = new ImageView();
        imageView.setId(id);
        return imageView;
    }

    // determine if this is a macOS retina display
    // https://stackoverflow.com/questions/20767708/how-do-you-detect-a-retina-display-in-java#20767802
    public static boolean isRetina() {
        return Screen.getPrimary().getOutputScaleX() > 1.5;
    }

    public static Image composeImage(String[] paths, int width, int height) {
        WritableImage image = new WritableImage(width, height);
        for (String path : paths) {
            Image part = getImageByPath("images/robohash/" + path, width, height);
            PixelReader reader = part.getPixelReader();
            PixelWriter writer = image.getPixelWriter();
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    Color color = reader.getColor(i, j);
                    if (color.isOpaque()) {
                        writer.setColor(i, j, color);
                    }
                }
            }
        }
        return image;
    }
}
