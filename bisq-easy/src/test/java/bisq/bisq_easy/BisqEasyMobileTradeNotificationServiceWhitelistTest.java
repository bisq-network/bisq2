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

package bisq.bisq_easy;

import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the trade-state whitelist + terminal-state mapping used by
 * {@link BisqEasyMobileTradeNotificationService}. Mirror of the Android nodeApp's
 * {@code OpenTradesNotificationService} state handler — the source of truth for
 * which trade transitions surface as mobile pushes.
 * <p>
 * See bisq-network/bisq-mobile#1450.
 */
public class BisqEasyMobileTradeNotificationServiceWhitelistTest {

    /**
     * Exactly mirrors {@code OpenTradesNotificationService.handleTradeStateNotification}
     * on Android nodeApp. Any change here must be matched there and vice-versa.
     */
    private static final Set<BisqEasyTradeState> EXPECTED_NON_TERMINAL_WHITELIST = EnumSet.of(
            BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION,
            BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
            BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
            BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT,
            BisqEasyTradeState.SELLER_SENT_BTC_SENT_CONFIRMATION,
            BisqEasyTradeState.BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
            BisqEasyTradeState.TAKER_SENT_TAKE_OFFER_REQUEST,
            BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_
    );

    @Test
    void whitelistCoversExactlyTheExpectedNonTerminalStates() {
        for (BisqEasyTradeState state : BisqEasyTradeState.values()) {
            boolean actual = BisqEasyMobileTradeNotificationService.isWhitelistedState(state);
            boolean expected = EXPECTED_NON_TERMINAL_WHITELIST.contains(state) || state.isFinalState();
            assertEquals(expected, actual,
                    () -> "Mobile-push eligibility mismatch for state " + state +
                            " — update the EXPECTED_NON_TERMINAL_WHITELIST in this test if the change is intentional, " +
                            "and mirror it in OpenTradesNotificationService on Android nodeApp.");
        }
    }

    @Test
    void allTerminalStatesAreImplicitlyWhitelisted() {
        for (BisqEasyTradeState state : BisqEasyTradeState.values()) {
            if (state.isFinalState()) {
                assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(state),
                        "Every terminal state should produce a push (via the terminal-fallback branch) — missed: " + state);
            }
        }
    }

    @Test
    void intermediateProtocolStatesAreSuppressed() {
        // INIT and intermediate "did_not_*" states are the kind that emit PROTOCOL_LOG_MESSAGE
        // noise on the chat path. They MUST NOT also fire a push here.
        assertFalse(BisqEasyMobileTradeNotificationService.isWhitelistedState(BisqEasyTradeState.INIT),
                "INIT is the protocol entry state, not a user-actionable event");
    }

    @Test
    void terminalI18nKeyResolvesForEachTerminalState() {
        // Each terminal state must have a resolvable label or the tradeCompleted message
        // will render with a null/missing translation argument.
        assertEquals("bisqEasy.mobileNotifications.terminal.completed",
                BisqEasyMobileTradeNotificationService.terminalI18nKey(BisqEasyTradeState.BTC_CONFIRMED));
        assertEquals("bisqEasy.mobileNotifications.terminal.rejected",
                BisqEasyMobileTradeNotificationService.terminalI18nKey(BisqEasyTradeState.REJECTED));
        assertEquals("bisqEasy.mobileNotifications.terminal.peerRejected",
                BisqEasyMobileTradeNotificationService.terminalI18nKey(BisqEasyTradeState.PEER_REJECTED));
        assertEquals("bisqEasy.mobileNotifications.terminal.cancelled",
                BisqEasyMobileTradeNotificationService.terminalI18nKey(BisqEasyTradeState.CANCELLED));
        assertEquals("bisqEasy.mobileNotifications.terminal.peerCancelled",
                BisqEasyMobileTradeNotificationService.terminalI18nKey(BisqEasyTradeState.PEER_CANCELLED));
        assertEquals("bisqEasy.mobileNotifications.terminal.failed",
                BisqEasyMobileTradeNotificationService.terminalI18nKey(BisqEasyTradeState.FAILED));
        assertEquals("bisqEasy.mobileNotifications.terminal.failedAtPeer",
                BisqEasyMobileTradeNotificationService.terminalI18nKey(BisqEasyTradeState.FAILED_AT_PEER));
    }

    @Test
    void terminalI18nKeyHasMappingForEveryTerminalState() {
        for (BisqEasyTradeState state : BisqEasyTradeState.values()) {
            String key = BisqEasyMobileTradeNotificationService.terminalI18nKey(state);
            if (state.isFinalState()) {
                assertNotNull(key,
                        "Terminal state " + state + " must map to an i18n key in terminalI18nKey()");
            } else {
                assertNull(key,
                        "Non-terminal state " + state + " must NOT map to a terminalI18nKey");
            }
        }
    }
}
