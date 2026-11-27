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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void testWriteAndReadRawImage(@TempDir Path tempDirPath) throws IOException {
        Image image = createInMemoryImage(8, 8);
        Path tempFilePath = tempDirPath.resolve("test.raw");

        CatHashImageUtil.writeRawImage(image, tempFilePath);
        Image readImage = CatHashImageUtil.readRawImage(tempFilePath);

        assertEquals(image.getWidth(), readImage.getWidth());
        assertEquals(image.getHeight(), readImage.getHeight());
    }

    @Test
    void testWrittenFileIsCompressedAndKeepsAllPixels(@TempDir Path tempDirPath) throws IOException {
        Image image = createPatternImage(120, 120);
        Path tempFilePath = tempDirPath.resolve("test.raw");

        CatHashImageUtil.writeRawImage(image, tempFilePath);
        byte[] fileData = Files.readAllBytes(tempFilePath);
        assertTrue(CatHashImageUtil.isCompressedFormat(fileData));
        assertTrue(fileData.length < CatHashImageUtil.imageToByteArray(image).length);

        assertSamePixels(image, CatHashImageUtil.readRawImage(tempFilePath));
    }

    @Test
    void testReadUncompressedFileAndReplaceItWithCompressedFile(@TempDir Path tempDirPath) throws IOException {
        Image image = createPatternImage(60, 60);
        byte[] rawData = CatHashImageUtil.imageToByteArray(image);
        assertFalse(CatHashImageUtil.isCompressedFormat(rawData));
        Path tempFilePath = tempDirPath.resolve("test.raw");
        Files.write(tempFilePath, rawData);

        assertSamePixels(image, CatHashImageUtil.readRawImage(tempFilePath));

        byte[] fileData = Files.readAllBytes(tempFilePath);
        assertTrue(CatHashImageUtil.isCompressedFormat(fileData));
        assertSamePixels(image, CatHashImageUtil.readRawImage(tempFilePath));
    }

    private Image createPatternImage(int width, int height) {
        WritableImage image = new WritableImage(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int alpha = (x + y) % 3 == 0 ? 0 : 0xFF;
                image.getPixelWriter().setArgb(x, y, alpha << 24 | (x * 2) << 16 | (y * 2) << 8 | (x + y) % 256);
            }
        }
        return image;
    }

    private void assertSamePixels(Image expected, Image actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        for (int x = 0; x < (int) expected.getWidth(); x++) {
            for (int y = 0; y < (int) expected.getHeight(); y++) {
                assertEquals(expected.getPixelReader().getArgb(x, y), actual.getPixelReader().getArgb(x, y));
            }
        }
    }
}