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
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the table itself. The exhaustive switch already guarantees a topic cannot be MISSING a
 * permission — the class does not compile otherwise — but nothing guarantees a row is not WRONG,
 * and a wrong row widens access silently: the subscription is served, the client is never told, and
 * only reading the switch reveals it.
 * <p>
 * Every expectation below is the permission that already guards the same data over REST, so a
 * client cannot reach through a subscription what a request would refuse it. {@code NETWORK_INFO}
 * is the one row with no REST route to mirror.
 */
class SubscriptionPermissionMappingTest {
    private static final Map<Topic, Permission> EXPECTED = new EnumMap<>(Map.ofEntries(
            Map.entry(Topic.MARKET_PRICE, Permission.MARKET_PRICE),
            Map.entry(Topic.NUM_OFFERS, Permission.OFFERBOOK),
            Map.entry(Topic.OFFERS, Permission.OFFERBOOK),
            Map.entry(Topic.TRADES, Permission.TRADES),
            Map.entry(Topic.TRADE_PROPERTIES, Permission.TRADES),
            Map.entry(Topic.TRADE_CHAT_MESSAGES, Permission.TRADE_CHAT_CHANNELS),
            Map.entry(Topic.CHAT_REACTIONS, Permission.TRADE_CHAT_CHANNELS),
            Map.entry(Topic.REPUTATION, Permission.REPUTATION),
            Map.entry(Topic.NUM_USER_PROFILES, Permission.USER_PROFILES),
            Map.entry(Topic.ALERT_NOTIFICATIONS, Permission.SETTINGS),
            Map.entry(Topic.TRADE_RESTRICTING_ALERT, Permission.SETTINGS),
            Map.entry(Topic.NETWORK_INFO, Permission.NETWORK_INFO),
            Map.entry(Topic.PRIVATE_CHAT_CHANNELS, Permission.PRIVATE_CHAT_CHANNELS),
            Map.entry(Topic.PRIVATE_CHAT_MESSAGES, Permission.PRIVATE_CHAT_CHANNELS),
            Map.entry(Topic.PRIVATE_CHAT_REACTIONS, Permission.PRIVATE_CHAT_CHANNELS),
            Map.entry(Topic.CONTACTS, Permission.CONTACTS),
            Map.entry(Topic.PUBLIC_CHAT_CHANNELS, Permission.PUBLIC_CHAT_CHANNELS),
            Map.entry(Topic.PUBLIC_CHAT_MESSAGES, Permission.PUBLIC_CHAT_CHANNELS),
            Map.entry(Topic.PUBLIC_CHAT_REACTIONS, Permission.PUBLIC_CHAT_CHANNELS)));

    private final SubscriptionPermissionMapping mapping = new SubscriptionPermissionMapping();

    @Test
    void everyTopicRequiresThePermissionGuardingTheSameDataOverRest() {
        for (Topic topic : Topic.values()) {
            assertThat(mapping.getRequiredPermission(topic))
                    .as("%s", topic)
                    .isEqualTo(EXPECTED.get(topic));
        }
    }

    @Test
    void everyTopicIsAccountedForHere() {
        // Without this the loop above would quietly stop covering a topic added later: the switch
        // would force the author to declare a permission, and this table would not force them to
        // agree that it is the right one.
        assertThat(EXPECTED.keySet()).containsExactlyInAnyOrder(Topic.values());
    }
}
