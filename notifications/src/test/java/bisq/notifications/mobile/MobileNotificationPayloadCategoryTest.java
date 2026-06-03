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

package bisq.notifications.mobile;

import bisq.common.json.JsonMapperProvider;
import bisq.notifications.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the wire contract between bisq2's {@link MobileNotificationPayload} and the mobile
 * client's {@code BisqFirebaseMessagingService.NotificationPayload} (Android) / iOS NSE
 * decoder. Specifically guards against:
 * <p>
 *   - Drift in the {@code category} id strings (mobile uses {@code "chat_message"},
 *     {@code "trade_update"}, {@code "offer_update"}, {@code "general"}).
 *   - Loss of forward-compatibility when newer bisq2 instances emit category ids the
 *     local enum doesn't know about — those must deserialize to
 *     {@link Notification.Category#GENERAL} rather than throwing.
 *   - A future refactor making {@code category} required on the wire, breaking older
 *     trusted nodes that don't yet override {@code Notification#getCategory()}.
 * <p>
 * Mirror for bisq-network/bisq-mobile#1450.
 */
class MobileNotificationPayloadCategoryTest {

    private final ObjectMapper mapper = JsonMapperProvider.get();

    @Test
    void chatCategoryRoundTripsThroughJson() throws Exception {
        MobileNotificationPayload original = new MobileNotificationPayload(
                "channel.msg-123",
                "Alice (Bisq Easy → Open Trades → Bob)",
                "hey, account info incoming",
                Notification.Category.CHAT_MESSAGE);

        String json = mapper.writeValueAsString(original);
        assertTrue(json.contains("\"category\":\"chat_message\""),
                "category must be serialized as the lowercase id (via @JsonValue) for the mobile client: " + json);

        MobileNotificationPayload decoded = mapper.readValue(json, MobileNotificationPayload.class);
        assertEquals(original, decoded);
        assertEquals(Notification.Category.CHAT_MESSAGE, decoded.getCategory());
    }

    @Test
    void tradeUpdateCategoryRoundTripsThroughJson() throws Exception {
        MobileNotificationPayload original = new MobileNotificationPayload(
                "trade-id.abcd",
                "Trade abcd1234",
                "Peer confirmed fiat receipt",
                Notification.Category.TRADE_UPDATE);

        String json = mapper.writeValueAsString(original);
        assertTrue(json.contains("\"category\":\"trade_update\""), json);

        MobileNotificationPayload decoded = mapper.readValue(json, MobileNotificationPayload.class);
        assertEquals(Notification.Category.TRADE_UPDATE, decoded.getCategory());
    }

    @Test
    void absentCategoryInJsonDefaultsToGeneralForOlderNodes() throws Exception {
        // Simulates a payload built by an older bisq2 (pre-#1450) where Notification
        // implementations didn't override getCategory(). The deserializer must accept
        // the absent field and default to GENERAL rather than throwing.
        String legacyJson = "{\"id\":\"x\",\"title\":\"t\",\"message\":\"m\"}";

        MobileNotificationPayload decoded = mapper.readValue(legacyJson, MobileNotificationPayload.class);

        assertEquals(Notification.Category.GENERAL, decoded.getCategory());
    }

    @Test
    void unknownCategoryIdFromNewerNodeDeserializesAsGeneralRatherThanThrowing() throws Exception {
        // Forward-compat: a newer bisq2 may introduce e.g. "dispute_alert" before
        // older instances / mobile clients learn the id. {@link Notification.Category#fromId}
        // returns GENERAL rather than throwing, so older receivers keep working
        // (they'll just show the generic banner instead of the new category).
        String futureJson = "{\"id\":\"x\",\"title\":\"t\",\"message\":\"m\",\"category\":\"dispute_alert\"}";

        MobileNotificationPayload decoded = mapper.readValue(futureJson, MobileNotificationPayload.class);

        assertEquals(Notification.Category.GENERAL, decoded.getCategory());
    }

    @Test
    void categoryIsAlwaysPresentOnTheWire() throws Exception {
        // Constructor normalises null → GENERAL, so the payload field is never null
        // at serialization time. The mobile client can therefore assume the JSON
        // always carries a `category` key — no `null` and no missing key.
        MobileNotificationPayload general = new MobileNotificationPayload("i", "t", "m", null);

        String json = mapper.writeValueAsString(general);
        assertTrue(json.contains("\"category\":\"general\""),
                "constructor normalises null → GENERAL; that explicit category must be on the wire: " + json);
        assertFalse(json.contains("\"category\":null"),
                "must never emit a null category to the wire: " + json);
    }

    @Test
    void categoryEnumIdsMatchMobileWireContract() {
        // The mobile client (BisqFirebaseMessagingService.NotificationCategory.CHAT_MESSAGE)
        // uses the literal id "chat_message" and compares against the on-wire value
        // produced by `Category#getId` (via @JsonValue). If these literals ever drift,
        // every chat push on every device version drops back to GENERAL silently — pin
        // the wire format here, not the enum constant names.
        assertEquals("chat_message", Notification.Category.CHAT_MESSAGE.getId());
        assertEquals("trade_update", Notification.Category.TRADE_UPDATE.getId());
        assertEquals("offer_update", Notification.Category.OFFER_UPDATE.getId());
        assertEquals("general", Notification.Category.GENERAL.getId());
    }
}
