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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class WebcamIpcWireMessageValidation {
    public static boolean verify(String sessionSecret, WebcamIpcWireMessage wireMessage) {
        checkArgument(sessionSecret != null && !sessionSecret.isBlank(), "sessionSecret must not be empty");
        checkNotNull(wireMessage, "wireMessage must not be null");
        checkArgument(wireMessage.getVersion() == WebcamIpcWireMessage.VERSION, "Unsupported message version");

        boolean isHmacValid = WebcamIpcAuthenticator.verifyHmac(sessionSecret,
                wireMessage.getSignaturePayload(),
                wireMessage.getHmac());
        checkArgument(isHmacValid, "Invalid message authentication");
        return true;
    }
}
