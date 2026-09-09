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

package bisq.desktop.common.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class QrCodeDisplay {
    private static final int BLACK_ARGB = 0xFF000000;
    private static final int WHITE_ARGB = 0xFFFFFFFF;

    public static Image toImage(String data, int size) {
        return toImage(data, size, 0);
    }

    public static Image toImage(String data, int size, int margin) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    data, BarcodeFormat.QR_CODE, size, size, Map.of(EncodeHintType.MARGIN, margin));
            WritableImage image = new WritableImage(matrix.getWidth(), matrix.getHeight());
            PixelWriter pixelWriter = image.getPixelWriter();
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    pixelWriter.setArgb(x, y, matrix.get(x, y) ? BLACK_ARGB : WHITE_ARGB);
                }
            }
            return image;
        } catch (WriterException e) {
            log.error("Failed to generate QR code", e);
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }
}
