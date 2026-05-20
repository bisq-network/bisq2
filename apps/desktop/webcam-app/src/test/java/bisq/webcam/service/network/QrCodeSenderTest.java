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

package bisq.webcam.service.network;

import bisq.common.webcam.WebcamControlSignals;
import bisq.common.webcam.WebcamIpcWireMessage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QrCodeSenderTest {
    @Test
    void sendReturnsFailedFutureWhenPayloadIsTooLarge() {
        QrCodeSender qrCodeSender = new QrCodeSender(0, "secret");
        String payload = "a".repeat(WebcamIpcWireMessage.MAX_PAYLOAD_LENGTH + 1);

        try {
            CompletableFuture<Void> future = assertDoesNotThrow(() ->
                    qrCodeSender.send(WebcamControlSignals.QR_CODE_PREFIX, payload));

            CompletionException exception = assertThrows(CompletionException.class, future::join);
            assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        } finally {
            qrCodeSender.shutdown();
        }
    }

    @Test
    void sendReturnsFailedFutureWhenPayloadIsNull() {
        QrCodeSender qrCodeSender = new QrCodeSender(0, "secret");

        try {
            CompletableFuture<Void> future = assertDoesNotThrow(() ->
                    qrCodeSender.send(WebcamControlSignals.QR_CODE_PREFIX, null));

            CompletionException exception = assertThrows(CompletionException.class, future::join);
            assertInstanceOf(NullPointerException.class, exception.getCause());
        } finally {
            qrCodeSender.shutdown();
        }
    }
}
