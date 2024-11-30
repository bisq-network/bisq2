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

import bisq.common.file.FileUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.*;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CatHashImageUtil {
    private static final String BASE_PATH = "images/cathash/";

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

    public static Image readRawImage(File file) throws IOException {
        byte[] rawData = FileUtils.read(file.getAbsolutePath());
        return byteArrayToImage(rawData);
    }

    public static void writeRawImage(Image image, File file) throws IOException {
        byte[] rawData = imageToByteArray(image);
        FileUtils.write(file.getAbsolutePath(), rawData);
    }

    public static Image byteArrayToImage(byte[] data) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);

        int width = byteArrayInputStream.read();
        int height = byteArrayInputStream.read();

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
