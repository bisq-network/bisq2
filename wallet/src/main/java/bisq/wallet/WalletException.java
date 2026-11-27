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

package bisq.wallet;

import lombok.Getter;

import java.util.Optional;

@Getter
public class WalletException extends RuntimeException {
    public enum Reason {
        WALLET_NOT_OPEN,
        WRONG_PASSWORD,
        INVALID_REQUEST,
        DAEMON_UNAVAILABLE,
        UNSUPPORTED_CALL,
        CLIENT_NOT_READY,
        DAEMON_ERROR
    }

    private final Reason reason;
    private final Optional<String> detail;

    public WalletException(Reason reason, String message, Optional<String> detail, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.detail = detail;
    }
}
