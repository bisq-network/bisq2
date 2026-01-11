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

package bisq.api.access.permissions;

import lombok.Getter;

/**
 * The id must not be changed as it is used for the serialisation.
 */
public enum Permission {
    TRADE_CHAT_CHANNELS(0),
    EXPLORER(1),
    MARKET_PRICE(2),
    OFFERBOOK(3),
    PAYMENT_ACCOUNTS(4),
    REPUTATION(5),
    SETTINGS(6),
    TRADES(7),
    USER_IDENTITIES(8),
    USER_PROFILES(9);

    @Getter
    private final int id;

    Permission(int id) {
        this.id = id;
    }

    public static Permission fromId(int id) {
        for (Permission permission : values()) {
            if (permission.id == id) return permission;
        }
        throw new IllegalArgumentException("No permission found for id " + id);
    }
}
