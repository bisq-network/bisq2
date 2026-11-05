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

package bisq.desktop.main.content.wallet;

import bisq.i18n.Res;
import bisq.wallet.WalletException;

import java.util.concurrent.CompletionException;

public class WalletOpenErrors {
    private WalletOpenErrors() {
    }

    public static String toMessage(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        if (cause instanceof WalletException walletException) {
            return switch (walletException.getReason()) {
                case WRONG_PASSWORD -> Res.get("wallet.protectWallet.error.wrongPassword");
                case DAEMON_UNAVAILABLE, CLIENT_NOT_READY -> Res.get("wallet.protectWallet.error.daemonUnavailable");
                case INVALID_REQUEST -> Res.get("wallet.protectWallet.error.openRefused",
                        walletException.getDetail().orElse(walletException.getMessage()));
                default -> Res.get("wallet.protectWallet.error.daemonError");
            };
        }
        return Res.get("wallet.protectWallet.error.daemonError");
    }
}
