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
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatHashImageUtilTest {

    private Image createInMemoryImage(int width, int height) {
        return new WritableImage(width, height);
    }

    @Test
    void testByteArrayToImageAndImageToByteArray() {
        Image image = createInMemoryImage(16, 16);
        WritableImage writableImage = (WritableImage) image;
        writableImage.getPixelWriter().setArgb(0, 0, 0xFF0000FF); // Red pixel
        byte[] bytes = CatHashImageUtil.imageToByteArray(image);
        Image result = CatHashImageUtil.byteArrayToImage(bytes);
        assertEquals(image.getWidth(), result.getWidth());
        assertEquals(image.getHeight(), result.getHeight());
        assertEquals(0xFF0000FF, result.getPixelReader().getArgb(0, 0));
    }

    @Test
    void testWriteAndReadRawImage(@TempDir Path tempDir) throws IOException {
        Image image = createInMemoryImage(8, 8);
        Path tempFile = tempDir.resolve("test.raw");

        CatHashImageUtil.writeRawImage(image, tempFile);
        Image readImage = CatHashImageUtil.readRawImage(tempFile);

        assertEquals(image.getWidth(), readImage.getWidth());
        assertEquals(image.getHeight(), readImage.getHeight());
    }
}