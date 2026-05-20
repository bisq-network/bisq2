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

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Getter
public final class WebcamIpcMessage {

    private final WebcamControlSignals signal;
    private final Optional<String> payload;

    WebcamIpcMessage(WebcamControlSignals signal, Optional<String> payload) {
        this.signal = checkNotNull(signal, "signal must not be null");
        this.payload = checkNotNull(payload, "payload must not be null");
    }

    public static WebcamIpcMessage fromWireMessage(WebcamIpcWireMessage wireMessage) {
        checkNotNull(wireMessage, "wireMessage must not be null");
        WebcamControlSignals signal = WebcamControlSignals.fromCode(wireMessage.getSignalCode());
        byte[] payloadBytes = wireMessage.getPayload();
        Optional<String> payload = payloadBytes.length == 0
                ? Optional.empty()
                : Optional.of(new String(payloadBytes, StandardCharsets.UTF_8));
        return new WebcamIpcMessage(signal, payload);
    }
}
