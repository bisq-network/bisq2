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

import jakarta.ws.rs.ForbiddenException;

import java.util.List;
import java.util.Optional;

public final class RestPermissionMapping implements PermissionMapping {
    private final List<PermissionRule> rules;

    public RestPermissionMapping() {
        // TODO apply rules to actual endpoints and methods. Atm we only check the root path
        this.rules = List.of(
                new PermissionRule("^/trade-chat-channels(/.*)?$", Optional.empty(), Permission.TRADE_CHAT_CHANNELS),
                new PermissionRule("^/explorer(/.*)?$", Optional.empty(), Permission.EXPLORER),
                new PermissionRule("^/market-price(/.*)?$", Optional.empty(), Permission.MARKET_PRICE),
                new PermissionRule("^/offerbook(/.*)?$", Optional.empty(), Permission.OFFERBOOK),
                new PermissionRule("^/payment-accounts(/.*)?$", Optional.empty(), Permission.PAYMENT_ACCOUNTS),
                new PermissionRule("^/reputation(/.*)?$", Optional.empty(), Permission.REPUTATION),
                new PermissionRule("^/settings(/.*)?$", Optional.empty(), Permission.SETTINGS),
                new PermissionRule("^/trades(/.*)?$", Optional.empty(), Permission.TRADES),
                new PermissionRule("^/user-identity(/.*)?$", Optional.empty(), Permission.USER_IDENTITIES),
                new PermissionRule("^/user-profile(/.*)?$", Optional.empty(), Permission.USER_PROFILES)
        );
    }

    @Override
    public Permission getRequiredPermission(String path, String method) {
        return rules.stream()
                .filter(rule -> rule.matches(path, method))
                .map(PermissionRule::permission)
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("No permission mapping for " + method + " " + path));
    }
}

