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

package bisq.network.p2p.node;

import lombok.Getter;

import java.util.concurrent.CompletionException;

public class ConnectionException extends CompletionException {
    public enum Reason {
        UNSPECIFIED,
        INVALID_NETWORK_VERSION,
        PROTOBUF_IS_NULL,
        AUTHORIZATION_FAILED,
        ONION_ADDRESS_VERIFICATION_FAILED,
        ADDRESS_BANNED,
        HANDSHAKE_FAILED
    }

    @Getter
    private Reason reason = Reason.UNSPECIFIED;

    public ConnectionException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(Reason reason, Throwable throwable) {
        super(throwable);
        this.reason = reason;
    }

    public ConnectionException(Throwable throwable) {
        super(throwable);
    }
}