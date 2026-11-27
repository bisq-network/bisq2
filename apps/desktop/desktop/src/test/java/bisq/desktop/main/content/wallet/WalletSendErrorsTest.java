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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletSendErrorsTest {
    @BeforeAll
    static void setUp() {
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    void wrongPasswordNamesThePassword() {
        WalletException exception = new WalletException(WalletException.Reason.WRONG_PASSWORD, "denied", Optional.empty(), null);

        assertEquals(Res.get("wallet.send.error.wrongPassword"), WalletSendErrors.toMessage(exception));
        assertEquals(Res.get("wallet.send.error.wrongPassword"), WalletSendErrors.toMessage(new CompletionException(exception)));
    }

    @Test
    void refusedRequestCarriesTheDaemonsDetail() {
        WalletException withDetail = new WalletException(WalletException.Reason.INVALID_REQUEST, "invalid",
                Optional.of("insufficient funds"), null);
        WalletException withoutDetail = new WalletException(WalletException.Reason.INVALID_REQUEST, "invalid",
                Optional.empty(), null);

        assertEquals(Res.get("wallet.send.error.refused", "insufficient funds"), WalletSendErrors.toMessage(withDetail));
        assertEquals(Res.get("wallet.send.error.refused", "invalid"), WalletSendErrors.toMessage(withoutDetail));
    }

    @Test
    void anInternalErrorWithADescriptionShowsTheDescription() {
        WalletException insufficientFunds = new WalletException(WalletException.Reason.DAEMON_ERROR, "internal",
                Optional.of("Insufficient funds: 49.24999430 BTC available of 100.00000199 BTC needed"), null);

        assertEquals(Res.get("wallet.send.error.failed", "Insufficient funds: 49.24999430 BTC available of 100.00000199 BTC needed")
                        + "\n\n" + Res.get("wallet.send.error.outcomeUnknown"),
                WalletSendErrors.toMessage(insufficientFunds));
    }

    @Test
    void otherFailuresFallBackToTheDaemonError() {
        WalletException daemonError = new WalletException(WalletException.Reason.DAEMON_ERROR, "boom", Optional.empty(), null);

        String unknown = "\n\n" + Res.get("wallet.send.error.outcomeUnknown");
        assertEquals(Res.get("wallet.protectWallet.error.daemonError") + unknown, WalletSendErrors.toMessage(daemonError));
        assertEquals(Res.get("wallet.protectWallet.error.daemonError") + unknown, WalletSendErrors.toMessage(new RuntimeException("boom")));
        assertEquals(Res.get("wallet.protectWallet.error.daemonUnavailable") + unknown, WalletSendErrors.toMessage(
                new WalletException(WalletException.Reason.DAEMON_UNAVAILABLE, "down", Optional.empty(), null)));
        assertEquals(Res.get("wallet.protectWallet.error.daemonUnavailable"), WalletSendErrors.toMessage(
                new WalletException(WalletException.Reason.CLIENT_NOT_READY, "not ready", Optional.empty(), null)));
    }

    @Test
    void onlyFailuresAfterWhichThePaymentMayHaveLeftHaveAnUnknownOutcome() {
        assertTrue(WalletSendErrors.isOutcomeUnknown(reason(WalletException.Reason.DAEMON_UNAVAILABLE)));
        assertTrue(WalletSendErrors.isOutcomeUnknown(reason(WalletException.Reason.DAEMON_ERROR)));
        assertTrue(WalletSendErrors.isOutcomeUnknown(reason(WalletException.Reason.CALL_TIMED_OUT)));
        assertTrue(WalletSendErrors.isOutcomeUnknown(new CompletionException(reason(WalletException.Reason.DAEMON_UNAVAILABLE))));
        assertTrue(WalletSendErrors.isOutcomeUnknown(new RuntimeException("boom")));

        assertFalse(WalletSendErrors.isOutcomeUnknown(reason(WalletException.Reason.WRONG_PASSWORD)));
        assertFalse(WalletSendErrors.isOutcomeUnknown(reason(WalletException.Reason.WALLET_NOT_OPEN)));
        assertFalse(WalletSendErrors.isOutcomeUnknown(reason(WalletException.Reason.INVALID_REQUEST)));
        assertFalse(WalletSendErrors.isOutcomeUnknown(reason(WalletException.Reason.CLIENT_NOT_READY)));
        assertFalse(WalletSendErrors.isOutcomeUnknown(reason(WalletException.Reason.UNSUPPORTED_CALL)));
    }

    private static WalletException reason(WalletException.Reason reason) {
        return new WalletException(reason, reason.name(), Optional.empty(), null);
    }
}
