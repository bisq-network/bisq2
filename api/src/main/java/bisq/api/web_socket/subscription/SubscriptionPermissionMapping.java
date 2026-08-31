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

package bisq.api.web_socket.subscription;

import bisq.api.access.permissions.Permission;

/**
 * Maps a subscription topic to the permission a client must hold to receive it.
 * <p>
 * Deliberately NOT a {@link bisq.api.access.permissions.PermissionMapping}: that interface answers
 * for a path and a method, and a topic is neither. Keeping both surfaces behind one path-shaped
 * interface is what let a WebSocket-only feature ship without ever being asked for a permission.
 * <p>
 * The switch is an expression with no {@code default} on purpose: a topic added without declaring
 * its permission does not compile. That is what makes the omission unrepeatable rather than
 * something each author has to remember.
 * <p>
 * Every row asks for the permission that already guards the same data over REST, so a client cannot
 * reach through a subscription what a request would refuse it. {@code NETWORK_INFO} is the one row
 * with no REST route to mirror; see {@link Permission#NETWORK_INFO}.
 */
public final class SubscriptionPermissionMapping {
    public Permission getRequiredPermission(Topic topic) {
        return switch (topic) {
            case MARKET_PRICE -> Permission.MARKET_PRICE;
            case NUM_OFFERS, OFFERS -> Permission.OFFERBOOK;
            case TRADES, TRADE_PROPERTIES -> Permission.TRADES;
            case TRADE_CHAT_MESSAGES, CHAT_REACTIONS -> Permission.TRADE_CHAT_CHANNELS;
            case REPUTATION -> Permission.REPUTATION;
            case NUM_USER_PROFILES -> Permission.USER_PROFILES;
            case ALERT_NOTIFICATIONS, TRADE_RESTRICTING_ALERT -> Permission.SETTINGS;
            case NETWORK_INFO -> Permission.NETWORK_INFO;
            case PRIVATE_CHAT_CHANNELS, PRIVATE_CHAT_MESSAGES, PRIVATE_CHAT_REACTIONS -> Permission.PRIVATE_CHAT_CHANNELS;
            case CONTACTS -> Permission.CONTACTS;
        };
    }
}
