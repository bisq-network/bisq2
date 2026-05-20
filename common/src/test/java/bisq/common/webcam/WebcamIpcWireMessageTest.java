/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WebcamIpcWireMessageTest {
    @Test
    void createRejectsPayloadOverMaxUtf8Bytes() {
        String payload = "\u20ac".repeat(334);

        assertThrows(IllegalArgumentException.class,
                () -> WebcamIpcWireMessage.create("secret", WebcamControlSignals.ERROR_MESSAGE_PREFIX, payload));
    }

    @Test
    void createAcceptsPayloadAtMaxUtf8Bytes() {
        String payload = "\u20ac".repeat(333) + "a";

        WebcamIpcWireMessage wireMessage = WebcamIpcWireMessage.create("secret", WebcamControlSignals.ERROR_MESSAGE_PREFIX, payload);

        assertEquals(WebcamIpcWireMessage.MAX_PAYLOAD_LENGTH, wireMessage.getPayload().length);
    }

    @Test
    void truncatesPayloadToMaxUtf8BytesWithoutSplittingCodePoint() {
        String payload = "\u20ac".repeat(334);

        String truncatedPayload = WebcamIpcWireMessage.truncatePayloadToMaxByteLength(payload);

        assertEquals("\u20ac".repeat(333), truncatedPayload);
    }
}
