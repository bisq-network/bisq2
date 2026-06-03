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

package bisq.chat.notifications;

import bisq.chat.ChatMessageType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the mobile-eligibility whitelist used by {@code ChatNotificationService} when
 * deciding whether to push a chat notification to the mobile relay (FCM / APNs).
 * <p>
 * Mirror of the proven Android nodeApp {@code OpenTradesNotificationService} behavior:
 * only {@code TEXT} and {@code TAKE_BISQ_EASY_OFFER} are surfaced as pushes; the
 * trade protocol log noise and other in-app indicators are desktop-only.
 * <p>
 * See bisq-network/bisq-mobile#1450.
 */
public class ChatNotificationServiceMobileEligibilityTest {

    private static final Set<ChatMessageType> EXPECTED_MOBILE_ELIGIBLE = EnumSet.of(
            ChatMessageType.TEXT,
            ChatMessageType.TAKE_BISQ_EASY_OFFER
    );

    @Test
    void textMessagesAreMobileEligible() {
        assertTrue(ChatNotificationService.isMobileEligible(ChatMessageType.TEXT),
                "Peer text chat messages must reach mobile push");
    }

    @Test
    void offerTakenMessagesAreMobileEligible() {
        assertTrue(ChatNotificationService.isMobileEligible(ChatMessageType.TAKE_BISQ_EASY_OFFER),
                "Offer-taken notifications must reach mobile push");
    }

    @Test
    void protocolLogMessagesAreSuppressedFromMobile() {
        assertFalse(ChatNotificationService.isMobileEligible(ChatMessageType.PROTOCOL_LOG_MESSAGE),
                "PROTOCOL_LOG_MESSAGE is the high-volume per-state-transition noise that motivated #1450 — must not reach mobile push");
    }

    @Test
    void inAppIndicatorsAreSuppressedFromMobile() {
        assertFalse(ChatNotificationService.isMobileEligible(ChatMessageType.LEAVE),
                "LEAVE events are in-app channel housekeeping, not user-facing pushes");
        assertFalse(ChatNotificationService.isMobileEligible(ChatMessageType.CHAT_RULES_WARNING),
                "CHAT_RULES_WARNING is a desktop in-app banner, not a push event");
        assertFalse(ChatNotificationService.isMobileEligible(ChatMessageType.EXPIRED_MESSAGES_INDICATOR),
                "EXPIRED_MESSAGES_INDICATOR is a desktop in-app marker, not a push event");
    }

    /**
     * Pins the FULL whitelist so that adding a new {@link ChatMessageType} forces a
     * deliberate decision: should it be a mobile push or not? Without this test,
     * a new enum value silently defaults to the {@code switch} compile error (good)
     * but a careless author could pick the wrong branch. This test fails loudly if
     * the whitelist changes and reminds the reviewer to update issue #1450 follow-ups.
     */
    @Test
    void mobileEligibleWhitelistIsExactlyTextAndOfferTaken() {
        for (ChatMessageType type : ChatMessageType.values()) {
            boolean actual = ChatNotificationService.isMobileEligible(type);
            boolean expected = EXPECTED_MOBILE_ELIGIBLE.contains(type);
            if (actual != expected) {
                throw new AssertionError(
                        "Mobile-eligibility whitelist changed for " + type +
                                " (expected=" + expected + ", actual=" + actual + "). " +
                                "If this is intentional, update EXPECTED_MOBILE_ELIGIBLE in this test " +
                                "and note the change in the #1450 follow-up.");
            }
        }
    }
}
