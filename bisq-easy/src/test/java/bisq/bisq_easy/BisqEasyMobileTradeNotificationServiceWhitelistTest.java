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
     * Mirrors the trade-state whitelist in
     * {@link BisqEasyMobileTradeNotificationService#handleStateChange} (kept in sync
     * with {@code OpenTradesNotificationService} on Android nodeApp).
     * <p>
     * Self-action states reached via a LOCAL event (BUYER_SENT_FIAT_SENT_CONFIRMATION,
     * SELLER_CONFIRMED_FIAT_RECEIPT, SELLER_SENT_BTC_SENT_CONFIRMATION,
     * MAKER_SENT_TAKE_OFFER_RESPONSE__*) are intentionally absent from the
     * whitelist — pushing the user about an action they just took on the device
     * is noise, and the counterparty already gets the push on their own machine
     * via the symmetric "*_RECEIVED_*" state. See bisq-network/bisq-mobile#1464.
     * <p>
     * The payment-info-exchange block lists all 10 states across the 4 trade roles
     * (buyer/seller × maker/taker) and 3 timing branches; the dedup keyed on
     * {@code notifiedPaymentInfo} collapses them to a single push per trade.
     */
    private static final Set<BisqEasyTradeState> EXPECTED_NON_TERMINAL_WHITELIST = EnumSet.of(
            BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
            BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
            BisqEasyTradeState.BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
            BisqEasyTradeState.TAKER_SENT_TAKE_OFFER_REQUEST,
            // Payment-info exchange — buyer-as-taker (Branch-1.2 + final converging)
            BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            // Payment-info exchange — seller-as-maker (Branch-1.2 + final converging)
            BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            // Payment-info exchange — seller-as-taker (Branch-1.1 + Branch-2 + final converging)
            BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            // Payment-info exchange — buyer-as-maker (Branch-1.2 + Branch-2 + final converging)
            BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_,
            BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
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

    /**
     * #1464 follow-up: states reached via a LOCAL event in the user's own FSM
     * must NOT push — pushing the user about an action they just took on the
     * device is noise. The counterparty still learns about each action on their
     * own machine via the symmetric "*_RECEIVED_*" state, so this is purely
     * about silencing self-notifications, not dropping signal.
     * <p>
     * Mirrors the foreground-gated behaviour of the nodeApp
     * {@code OpenTradesNotificationService} — the relay path has no foreground
     * signal, so we suppress at the source instead.
     */
    @Test
    void selfActionStatesAreSuppressedFromMobile() {
        assertFalse(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                        BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION),
                "BUYER_SENT_FIAT_SENT_CONFIRMATION is reached via the buyer's local BisqEasyConfirmFiatSentEvent — must not self-notify");
        assertFalse(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                        BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT),
                "SELLER_CONFIRMED_FIAT_RECEIPT is the seller's local 'I received fiat' event — must not self-notify");
        assertFalse(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                        BisqEasyTradeState.SELLER_SENT_BTC_SENT_CONFIRMATION),
                "SELLER_SENT_BTC_SENT_CONFIRMATION is the seller's local 'I sent BTC' event — must not self-notify");
        assertFalse(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS),
                "MAKER_SENT_TAKE_OFFER_RESPONSE__* is the maker's local response event — already pushed via TAKER_SENT_TAKE_OFFER_REQUEST");
        assertFalse(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                        BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA),
                "MAKER_SENT_TAKE_OFFER_RESPONSE__* is the maker's local response event — already pushed via TAKER_SENT_TAKE_OFFER_REQUEST");
    }

    /**
     * Peer-action states (the "*_RECEIVED_*" twins of the self-suppressed states)
     * remain whitelisted — those are the meaningful signals.
     */
    @Test
    void peerActionStatesStillFireForCorrectRole() {
        assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                        BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION),
                "Seller must still be notified when buyer sends fiat (peer action)");
        assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                        BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION),
                "Buyer must still be notified when seller confirms receipt (peer action)");
        assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                        BisqEasyTradeState.BUYER_RECEIVED_BTC_SENT_CONFIRMATION),
                "Buyer must still be notified when seller sends BTC (peer action)");
        assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                        BisqEasyTradeState.TAKER_SENT_TAKE_OFFER_REQUEST),
                "Maker must still be notified when taker takes the offer (peer action; gated to maker in dispatch)");
    }

    /**
     * Regression for bisq-network/bisq-mobile#1464 — buyer-as-taker sends BTC
     * address first, then the seller sends account data while the app is
     * backgrounded. Before the fix, the resulting state
     * ({@code TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA},
     * the "final converging" state of Branch 1.1) was NOT whitelisted, so the
     * trade-state observer produced no mobile push and the buyer never learned
     * payment info had arrived.
     * <p>
     * The same gap existed for the symmetric final-converging states of the
     * other three roles (seller-as-maker, seller-as-taker, buyer-as-maker),
     * plus the seller-as-maker / seller-as-taker / buyer-as-maker Branch-1.x
     * intermediates that the original 3-state whitelist also missed.
     */
    @Test
    void paymentInfoExchangeFiresForBuyerAsTakerWhoSentBtcAddressFirst() {
        assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                        BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA),
                "buyer-as-taker who sent btc address first must still get a push when account data arrives (#1464)");
    }

    @Test
    void paymentInfoExchangeFiresForAllFourFinalConvergingStates() {
        // The "both halves done" state — reached when either party completes the
        // exchange after the other side has already sent its half — must always
        // trigger a push (deduped) regardless of role / ordering.
        assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA),
                "buyer-as-taker final converging");
        assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS),
                "seller-as-maker final converging");
        assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS),
                "seller-as-taker final converging");
        assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA),
                "buyer-as-maker final converging");
    }

    @Test
    void sellerAsMakerSendingAccountDataFirstFiresPush() {
        // Before #1464, seller-as-maker had NO state at all in the payment-info
        // whitelist — neither Branch-1.2 nor final converging — so they never
        // got a "payment info sent" push regardless of ordering. Pin both.
        assertTrue(BisqEasyMobileTradeNotificationService.isWhitelistedState(
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS),
                "seller-as-maker who sent account data before receiving btc address must still get a push (#1464)");
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
