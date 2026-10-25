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

import bisq.common.file.FileMutatorUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static com.google.common.base.Preconditions.checkArgument;

public class CatHashImageUtil {
    private static final String BASE_PATH = "images/cathash/";

    // A cache file in the compressed format starts with this marker, followed by the deflate compressed output of
    // imageToByteArray. A file in the older uncompressed format starts with the image width, which is never 0. So the
    // first byte tells the two formats apart.
    private static final byte[] COMPRESSED_FORMAT_MARKER = new byte[]{0, 1};

    public static Image composeImage(String[] paths, double size) {
        return composeImage(paths, size, size);
    }

    public static Image composeImage(String[] paths, double width, double height) {
        return composeImage(paths, BASE_PATH, width, height);
    }

    public static Image composeImage(String[] paths, String basePath, double width, double height) {
        Canvas canvas = new Canvas();
        canvas.setWidth(width);
        canvas.setHeight(height);
        GraphicsContext graphicsContext2D = canvas.getGraphicsContext2D();

        double radius = Math.min(height, width) / 2d;
        double x = width / 2d;
        double y = height / 2d;
        graphicsContext2D.beginPath();
        graphicsContext2D.moveTo(x - radius, y);
        graphicsContext2D.arc(x, y, radius, radius, 180, 360);
        graphicsContext2D.closePath();
        graphicsContext2D.clip();

        for (String path : paths) {
            graphicsContext2D.drawImage(new Image(basePath + path), 0, 0, width, height);
        }
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(snapshotParameters, null);
    }

    public static Image readRawImage(Path filePath) throws IOException {
        byte[] fileData = Files.readAllBytes(filePath);
        checkArgument(fileData.length > 0, "fileData must not be empty at readRawImage");
        if (isCompressedFormat(fileData)) {
            return byteArrayToImage(decompress(fileData));
        }

        Image image = byteArrayToImage(fileData);
        // Replace a file in the older uncompressed format, so that an existing cache also shrinks.
        writeRawImage(image, filePath);
        return image;
    }

    public static void writeRawImage(Image image, Path filePath) throws IOException {
        byte[] rawData = imageToByteArray(image);
        FileMutatorUtils.writeToPath(compress(rawData), filePath);
    }

    static boolean isCompressedFormat(byte[] fileData) {
        return fileData.length > COMPRESSED_FORMAT_MARKER.length &&
                Arrays.equals(fileData, 0, COMPRESSED_FORMAT_MARKER.length,
                        COMPRESSED_FORMAT_MARKER, 0, COMPRESSED_FORMAT_MARKER.length);
    }

    static byte[] compress(byte[] rawData) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(COMPRESSED_FORMAT_MARKER);
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream)) {
            deflaterOutputStream.write(rawData);
        }
        return byteArrayOutputStream.toByteArray();
    }

    static byte[] decompress(byte[] fileData) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileData,
                COMPRESSED_FORMAT_MARKER.length,
                fileData.length - COMPRESSED_FORMAT_MARKER.length);
        try (InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream)) {
            return inflaterInputStream.readAllBytes();
        }
    }

    public static Image byteArrayToImage(byte[] data) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);

        int width = byteArrayInputStream.read();
        int height = byteArrayInputStream.read();
        checkArgument(width > 0, "width must be > 0 at byteArrayToImage");
        checkArgument(height > 0, "height must be > 0 at byteArrayToImage");

        byte[] pixels = new byte[width * height * 4];
        byteArrayInputStream.read(pixels, 0, pixels.length);

        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();
        pixelWriter.setPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), pixels, 0, width * 4);

        return image;
    }

    public static byte[] imageToByteArray(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader pixelReader = image.getPixelReader();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(width);
        byteArrayOutputStream.write(height);

        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4); // 4 bytes per pixel (ARGB)
        WritablePixelFormat<ByteBuffer> format = WritablePixelFormat.getByteBgraInstance();
        pixelReader.getPixels(0, 0, width, height, format, buffer, width * 4);

        byteArrayOutputStream.write(buffer.array(), 0, buffer.array().length);
        return byteArrayOutputStream.toByteArray();
    }
}
