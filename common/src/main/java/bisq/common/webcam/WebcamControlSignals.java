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

package bisq.common.webcam;

import lombok.Getter;

public enum WebcamControlSignals {
    HEART_BEAT(1),
    IMAGE_RECOGNIZED(2),
    QR_CODE_PREFIX(3),
    ERROR_MESSAGE_PREFIX(4),
    RESTART(5),
    SHUTDOWN(6);

    @Getter
    private final byte code;

    WebcamControlSignals(int code) {
        this.code = (byte) code;
    }

    public static WebcamControlSignals fromCode(byte code) {
        for (WebcamControlSignals signal : values()) {
            if (signal.code == code) {
                return signal;
            }
        }
        throw new IllegalArgumentException("Unsupported webcam IPC signal code " + Byte.toUnsignedInt(code));
    }
}
