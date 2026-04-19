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

package bisq.api.access.pairing.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Map;

public class TextQrCodeRenderer {
    private static final char FULL = '\u2588';   // █ both rows on
    private static final char UPPER = '\u2580';  // ▀ upper only
    private static final char LOWER = '\u2584';  // ▄ lower only
    private static final char EMPTY = ' ';

    public static String render(String data) throws WriterException {
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 0, 0, hints);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        StringBuilder sb = new StringBuilder(width * (height / 2 + 1));
        for (int y = 0; y < height; y += 2) {
            for (int x = 0; x < width; x++) {
                boolean top = matrix.get(x, y);
                boolean bottom = y + 1 < height && matrix.get(x, y + 1);
                sb.append(top && bottom ? FULL : top ? UPPER : bottom ? LOWER : EMPTY);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
