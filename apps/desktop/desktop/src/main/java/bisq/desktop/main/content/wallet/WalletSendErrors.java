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

public class WalletSendErrors {
    private WalletSendErrors() {
    }

    public static String toMessage(Throwable throwable) {
        return WalletOpenErrors.findWalletException(throwable)
                .map(walletException -> switch (walletException.getReason()) {
                    case WRONG_PASSWORD -> Res.get("wallet.send.error.wrongPassword");
                    case WALLET_NOT_OPEN -> Res.get("wallet.send.error.walletNotOpen");
                    case DAEMON_UNAVAILABLE, CLIENT_NOT_READY -> Res.get("wallet.protectWallet.error.daemonUnavailable");
                    case CALL_TIMED_OUT -> Res.get("wallet.protectWallet.error.timedOut");
                    case INVALID_REQUEST -> Res.get("wallet.send.error.refused",
                            walletException.getDetail().orElse(walletException.getMessage()));
                    // The daemon reports insufficient funds as an internal error with a readable description
                    default -> walletException.getDetail()
                            .map(detail -> Res.get("wallet.send.error.failed", detail))
                            .orElse(Res.get("wallet.protectWallet.error.daemonError"));
                })
                .orElse(Res.get("wallet.protectWallet.error.daemonError"));
    }
}
