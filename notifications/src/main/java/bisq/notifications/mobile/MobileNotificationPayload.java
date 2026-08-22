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

import bisq.notifications.Notification;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class MobileNotificationPayload {
    private final String id;
    private final String title;
    private final String message;
    /**
     * Stable category mirroring the mobile client's {@code NotificationCategory#id}.
     * Lets the client route/label notifications without relying on the title-keyword
     * heuristic, which mislabels e.g. trade-private chats (whose title contains
     * "Open Trades") as trade updates. See bisq-network/bisq-mobile#1450.
     * <p>
     * Serialized via {@link Notification.Category}'s {@code @JsonValue} as the
     * lowercase id (e.g. {@code "chat_message"}) — the on-wire format the mobile
     * client compares against. Null-on-wire and unknown ids both deserialize to
     * {@link Notification.Category#GENERAL} (forward-compat for older clients
     * that don't emit the field, and newer producers that introduce new ids).
     */
    private final Notification.Category category;
    /**
     * Optional bisq2 trade id. When present, the mobile client routes a tap on
     * the push notification straight to the trade screen
     * ({@code bisq://OpenTrade/<tradeId>}) instead of the generic open-trade
     * list. Omitted from the wire payload when {@code null} so older mobile
     * clients (that don't parse this field) are unaffected.
     * <p>
     * See bisq-network/bisq-mobile#1395.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String tradeId;

    @JsonCreator
    public MobileNotificationPayload(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("message") String message,
            @JsonProperty("category") Notification.Category category,
            @JsonProperty("tradeId") String tradeId
    ) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.category = category == null ? Notification.Category.GENERAL : category;
        this.tradeId = tradeId;
    }
}
