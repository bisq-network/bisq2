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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the wire contract between bisq2's {@link MobileNotificationPayload} and the mobile
 * client's {@code BisqFirebaseMessagingService.NotificationPayload} (Android) / iOS NSE
 * decoder — every field the client parses, plus which of them actually ship.
 * <p>
 * On {@code category}, guards against:
 * <p>
 *   - Drift in the id strings (mobile uses {@code "chat_message"}, {@code "trade_update"},
 *     {@code "offer_update"}, {@code "general"}).
 *   - Loss of forward-compatibility when newer bisq2 instances emit category ids the
 *     local enum doesn't know about — those must deserialize to
 *     {@link Notification.Category#GENERAL} rather than throwing.
 *   - A future refactor making {@code category} required on the wire, breaking older
 *     trusted nodes that don't yet override {@code Notification#getCategory()}.
 * <p>
 * On the routing ids {@code tradeId} and {@code channelId}: both must vanish from the JSON
 * when absent, so clients that predate them see the payload shape they already know; and
 * {@link MobileNotificationPayload#from(Notification)} must drop the channel whenever a trade
 * id is present, which is the precedence rule this side enforces instead of trusting clients.
 * <p>
 * Mirror for bisq-network/bisq-mobile#1450 and #1395.
 */
class MobileNotificationPayloadTest {

    private final ObjectMapper mapper = JsonMapperProvider.get();

    @Test
    void chatCategoryRoundTripsThroughJson() throws Exception {
        MobileNotificationPayload original = new MobileNotificationPayload(
                "channel.msg-123",
                "Alice (Bisq Easy → Open Trades → Bob)",
                "hey, account info incoming",
                Notification.Category.CHAT_MESSAGE,
                null,
                null,
                null);

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
                Notification.Category.TRADE_UPDATE,
                null,
                null,
                null);

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
        MobileNotificationPayload general = new MobileNotificationPayload("i", "t", "m", null, null, null, null);

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

    // ---------- tradeId (bisq-network/bisq-mobile#1395) ----------

    @Test
    void tradeIdRoundTripsThroughJsonWhenPresent() throws Exception {
        MobileNotificationPayload original = new MobileNotificationPayload(
                "bisq-easy-mobile-trade-abcd",
                "Trade abcd1234",
                "Peer confirmed fiat receipt",
                Notification.Category.TRADE_UPDATE,
                "trade-id-abcd-1234",
                null,
                null);

        String json = mapper.writeValueAsString(original);
        assertTrue(json.contains("\"tradeId\":\"trade-id-abcd-1234\""),
                "tradeId must be serialized when present so the mobile client can deep-link: " + json);

        MobileNotificationPayload decoded = mapper.readValue(json, MobileNotificationPayload.class);
        assertEquals("trade-id-abcd-1234", decoded.getTradeId());
        assertEquals(original, decoded);
    }

    @Test
    void tradeIdIsOmittedFromWireWhenNullForBackwardCompatibility() throws Exception {
        // Older mobile clients (pre-#1395) don't parse `tradeId`. Emitting the key
        // as `null` would be tolerated, but omitting it entirely keeps the wire
        // payload identical to what those clients have always seen. This is what
        // the @JsonInclude(NON_NULL) annotation guarantees.
        MobileNotificationPayload payload = new MobileNotificationPayload(
                "id", "title", "message", Notification.Category.GENERAL, null, null, null);

        String json = mapper.writeValueAsString(payload);
        assertFalse(json.contains("tradeId"),
                "tradeId must NOT appear on the wire when null — older clients " +
                        "must see exactly the pre-#1395 payload shape: " + json);
    }

    @Test
    void absentTradeIdInJsonDeserializesAsNull() throws Exception {
        // Mirror of the older-bisq2 scenario: a payload produced before #1395
        // arrives at a new mobile client. The deserializer must treat the
        // missing field as null (not throw) so the mobile client falls back to
        // category-based routing.
        String legacyJson =
                "{\"id\":\"x\",\"title\":\"t\",\"message\":\"m\",\"category\":\"trade_update\"}";

        MobileNotificationPayload decoded = mapper.readValue(legacyJson, MobileNotificationPayload.class);

        assertNull(decoded.getTradeId());
        assertEquals(Notification.Category.TRADE_UPDATE, decoded.getCategory());
    }

    // ---------- channelId (private chat push) ----------

    @Test
    void channelIdRoundTripsThroughJsonWhenPresent() throws Exception {
        MobileNotificationPayload original = new MobileNotificationPayload(
                "discussion.alice-bob.msg-1",
                "Alice (Discussion → Private message)",
                "hey",
                Notification.Category.CHAT_MESSAGE,
                null,
                "discussion.alice-bob",
                null);

        String json = mapper.writeValueAsString(original);
        assertTrue(json.contains("\"channelId\":\"discussion.alice-bob\""),
                "channelId must be serialized when present so a DM push can open the conversation: " + json);

        MobileNotificationPayload decoded = mapper.readValue(json, MobileNotificationPayload.class);
        assertEquals("discussion.alice-bob", decoded.getChannelId());
        assertEquals(original, decoded);
    }

    @Test
    void channelIdIsOmittedFromWireWhenNullForBackwardCompatibility() throws Exception {
        MobileNotificationPayload payload = new MobileNotificationPayload(
                "id", "title", "message", Notification.Category.GENERAL, null, null, null);

        String json = mapper.writeValueAsString(payload);
        assertFalse(json.contains("channelId"),
                "channelId must NOT appear on the wire when null — clients that predate " +
                        "the private-chat relay must see exactly the payload shape they know: " + json);
    }

    @Test
    void absentChannelIdInJsonDeserializesAsNull() throws Exception {
        // A payload produced by a bisq2 that predates the private-chat relay arrives at
        // a new mobile client. The missing field must read as null so the client falls
        // back to its previous routing instead of throwing.
        String legacyJson =
                "{\"id\":\"x\",\"title\":\"t\",\"message\":\"m\",\"category\":\"chat_message\"}";

        MobileNotificationPayload decoded = mapper.readValue(legacyJson, MobileNotificationPayload.class);

        assertNull(decoded.getChannelId());
        assertEquals(Notification.Category.CHAT_MESSAGE, decoded.getCategory());
    }

    // ---------- from(Notification): which ids actually reach the wire ----------

    @Test
    void channelIdIsDroppedForATradeChatBecauseTheTradeIdAlreadyRoutesIt() {
        // A trade chat notification reports both. Only the trade id identifies anything
        // the client acts on, so the channel must not reach the wire. Pinning it here is
        // the whole point: the precedence is a rule this side enforces, not a convention
        // every present and future client is trusted to follow.
        MobileNotificationPayload payload = MobileNotificationPayload.from(
                notification("trade-abcd-1234", "bisq-easy-open-trades.trade-abcd-1234"));

        assertEquals("trade-abcd-1234", payload.getTradeId());
        assertNull(payload.getChannelId());
    }

    @Test
    void channelIdReachesTheWireForAPrivateMessage() {
        MobileNotificationPayload payload = MobileNotificationPayload.from(
                notification(null, "discussion.alice-bob"));

        assertNull(payload.getTradeId());
        assertEquals("discussion.alice-bob", payload.getChannelId());
    }

    @Test
    void neitherIdIsInventedWhenTheNotificationHasNone() {
        MobileNotificationPayload payload = MobileNotificationPayload.from(notification(null, null));

        assertNull(payload.getTradeId());
        assertNull(payload.getChannelId());
        assertEquals(Notification.Category.GENERAL, payload.getCategory());
    }

    // ---------- peerUserName (banner composed by the client) ----------

    @Test
    void peerUserNameRoundTripsThroughJsonWhenPresent() throws Exception {
        MobileNotificationPayload original = new MobileNotificationPayload(
                "discussion.alice-bob.msg-1",
                "Alice (Discussion → Private message)",
                "hey",
                Notification.Category.CHAT_MESSAGE,
                null,
                "discussion.alice-bob",
                "Alice");

        String json = mapper.writeValueAsString(original);
        assertTrue(json.contains("\"peerUserName\":\"Alice\""),
                "peerUserName must be serialized when present so the client can name the sender: " + json);

        MobileNotificationPayload decoded = mapper.readValue(json, MobileNotificationPayload.class);
        assertEquals("Alice", decoded.getPeerUserName());
        assertEquals(original, decoded);
    }

    @Test
    void peerUserNameIsOmittedFromWireWhenNullForBackwardCompatibility() throws Exception {
        MobileNotificationPayload payload = new MobileNotificationPayload(
                "id", "title", "message", Notification.Category.TRADE_UPDATE, "trade-1", null, null);

        String json = mapper.writeValueAsString(payload);
        assertFalse(json.contains("peerUserName"),
                "peerUserName must NOT appear on the wire when null — a client that predates it " +
                        "must see exactly the payload shape it knows: " + json);
    }

    @Test
    void absentPeerUserNameInJsonDeserializesAsNull() throws Exception {
        // A payload from a bisq2 that predates this field. The client falls back to its
        // category-only banner rather than throwing.
        String legacyJson =
                "{\"id\":\"x\",\"title\":\"t\",\"message\":\"m\",\"category\":\"chat_message\"}";

        MobileNotificationPayload decoded = mapper.readValue(legacyJson, MobileNotificationPayload.class);

        assertNull(decoded.getPeerUserName());
        assertEquals(Notification.Category.CHAT_MESSAGE, decoded.getCategory());
    }

    @Test
    void peerUserNameReachesTheWireAlongsideEitherRoutingId() {
        // Unlike channelId, this is not subject to any precedence rule: a trade chat needs the
        // sender's name just as much as a DM does, and both carry it to the same banner.
        MobileNotificationPayload dm = MobileNotificationPayload.from(
                notification(null, "discussion.alice-bob", "Alice"));
        MobileNotificationPayload tradeChat = MobileNotificationPayload.from(
                notification("trade-abcd-1234", "bisq-easy-open-trades.trade-abcd-1234", "Alice"));

        assertEquals("Alice", dm.getPeerUserName());
        assertEquals("Alice", tradeChat.getPeerUserName());
    }

    @Test
    void aNotificationWithoutACounterpartyReportsNoName() {
        // What a trade update looks like: MobileTradeNotification does not override
        // getPeerUserName, so the field never accompanies one.
        MobileNotificationPayload payload = MobileNotificationPayload.from(notification("trade-1", null));

        assertNull(payload.getPeerUserName());
    }

    /** Minimal stand-in — {@link Notification} declares only three abstract methods. */
    private static Notification notification(String tradeId, String channelId) {
        return notification(tradeId, channelId, null);
    }

    private static Notification notification(String tradeId, String channelId, String peerUserName) {
        return new Notification() {
            @Override
            public Optional<String> getPeerUserName() {
                return Optional.ofNullable(peerUserName);
            }

            @Override
            public String getId() {
                return "notification-id";
            }

            @Override
            public String getTitle() {
                return "title";
            }

            @Override
            public String getMessage() {
                return "message";
            }

            @Override
            public Optional<String> getTradeId() {
                return Optional.ofNullable(tradeId);
            }

            @Override
            public Optional<String> getChannelId() {
                return Optional.ofNullable(channelId);
            }
        };
    }
}
