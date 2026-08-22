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

import bisq.chat.ChatChannelDomain;
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
 * Two orthogonal axes are checked:
 * <ol>
 *   <li>{@link ChatMessageType} — only {@code TEXT} and {@code TAKE_BISQ_EASY_OFFER}
 *       are pushable (the {@code PROTOCOL_LOG_MESSAGE} noise filter from
 *       {@code bisq-network/bisq-mobile#1450}).</li>
 *   <li>{@link ChatChannelDomain} — only the user's private trade channels
 *       ({@code BISQ_EASY_OPEN_TRADES}, {@code MU_SIG_OPEN_TRADES}) push. Public
 *       chat domains (offerbook, discussion, support) stay desktop-only, fixing
 *       the "global community chat keeps notifying my phone after my trade
 *       closed" bug reported in {@code bisq-network/bisq-mobile#1464}.</li>
 * </ol>
 * Both checks must pass for a message to reach the mobile relay.
 */
public class ChatNotificationServiceMobileEligibilityTest {

    private static final Set<ChatMessageType> EXPECTED_MOBILE_ELIGIBLE_TYPES = EnumSet.of(
            ChatMessageType.TEXT,
            ChatMessageType.TAKE_BISQ_EASY_OFFER
    );

    private static final Set<ChatChannelDomain> EXPECTED_MOBILE_ELIGIBLE_DOMAINS = EnumSet.of(
            ChatChannelDomain.BISQ_EASY_OPEN_TRADES,
            ChatChannelDomain.MU_SIG_OPEN_TRADES
    );


    /* --------------------------------------------------------------------- */
    // ChatMessageType axis
    /* --------------------------------------------------------------------- */

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
     * Pins the FULL message-type whitelist so adding a new {@link ChatMessageType}
     * forces a deliberate decision: should it be a mobile push or not? Without
     * this test, a new enum value would silently inherit whichever branch a
     * careless author picked. Fails loudly so the reviewer remembers to revisit
     * the #1450 / #1464 follow-ups.
     */
    @Test
    void mobileEligibleTypeWhitelistIsExactlyTextAndOfferTaken() {
        for (ChatMessageType type : ChatMessageType.values()) {
            boolean actual = ChatNotificationService.isMobileEligible(type);
            boolean expected = EXPECTED_MOBILE_ELIGIBLE_TYPES.contains(type);
            if (actual != expected) {
                throw new AssertionError(
                        "Mobile-eligibility (type) whitelist changed for " + type +
                                " (expected=" + expected + ", actual=" + actual + "). " +
                                "If this is intentional, update EXPECTED_MOBILE_ELIGIBLE_TYPES in this test " +
                                "and note the change in the #1450 / #1464 follow-up.");
            }
        }
    }


    /* --------------------------------------------------------------------- */
    // ChatChannelDomain axis (#1464)
    /* --------------------------------------------------------------------- */

    @Test
    void bisqEasyOpenTradeChannelsAreMobileEligible() {
        assertTrue(ChatNotificationService.isMobileEligible(ChatChannelDomain.BISQ_EASY_OPEN_TRADES),
                "Bisq Easy private trade chat is the user's main signal — must reach mobile push");
    }

    @Test
    void muSigOpenTradeChannelsAreMobileEligible() {
        assertTrue(ChatNotificationService.isMobileEligible(ChatChannelDomain.MU_SIG_OPEN_TRADES),
                "MuSig private trade chat must reach mobile push — kept in the allowlist for when the mobile relay is wired up for MuSig");
    }

    @Test
    void publicOfferbookChannelsAreSuppressedFromMobile() {
        assertFalse(ChatNotificationService.isMobileEligible(ChatChannelDomain.BISQ_EASY_OFFERBOOK),
                "Bisq Easy offerbook chat is global per-market chatter, not a user-facing trade signal — desktop only");
    }

    @Test
    void globalDiscussionChannelsAreSuppressedFromMobile() {
        assertFalse(ChatNotificationService.isMobileEligible(ChatChannelDomain.DISCUSSION),
                "DISCUSSION is the global Bisq community chat — exactly the source of the post-trade #1464 noise; desktop only");
    }

    @Test
    void supportChannelsAreSuppressedFromMobile() {
        assertFalse(ChatNotificationService.isMobileEligible(ChatChannelDomain.SUPPORT),
                "SUPPORT is the public Bisq support channel — not the user's trade context; desktop only");
    }

    @Test
    void deprecatedDomainsAreSuppressedFromMobile() {
        //noinspection deprecation
        assertFalse(ChatNotificationService.isMobileEligible(ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT),
                "BISQ_EASY_PRIVATE_CHAT is deprecated (migrates to DISCUSSION) — never push");
        //noinspection deprecation
        assertFalse(ChatNotificationService.isMobileEligible(ChatChannelDomain.EVENTS),
                "EVENTS is deprecated (migrates to DISCUSSION) — never push");
    }

    /**
     * Same loud-failure pin as the type axis: a new {@link ChatChannelDomain}
     * must be classified explicitly. The exhaustive switch in
     * {@code ChatNotificationService} already gives a compile error, but this
     * test catches the silent case where a developer adds the new case but
     * forgets to update the mobile-push policy.
     */
    @Test
    void mobileEligibleDomainWhitelistIsExactlyTradeChannels() {
        for (ChatChannelDomain domain : ChatChannelDomain.values()) {
            boolean actual = ChatNotificationService.isMobileEligible(domain);
            boolean expected = EXPECTED_MOBILE_ELIGIBLE_DOMAINS.contains(domain);
            if (actual != expected) {
                throw new AssertionError(
                        "Mobile-eligibility (domain) whitelist changed for " + domain +
                                " (expected=" + expected + ", actual=" + actual + "). " +
                                "If this is intentional, update EXPECTED_MOBILE_ELIGIBLE_DOMAINS in this test " +
                                "and note the change in the #1464 follow-up.");
            }
        }
    }
}
