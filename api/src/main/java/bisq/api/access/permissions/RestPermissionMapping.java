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
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * Maps REST paths to required permissions. Unmapped paths are rejected (fail-closed).
 * <p>
 * For devs adding new endpoints or permissions:
 * <ul>
 * <li>Prefer mapping a new endpoint to an existing permission when one fits semantically —
 *     established precedent: {@code /trade-restricting-alert} and {@code /alert-notifications}
 *     map to {@link Permission#SETTINGS}.</li>
 * <li>When adding a new {@link Permission} value, ids are append-only and must never be reused.
 *     Already-paired clients holding a grantAll grant gain new non-sensitive permissions
 *     automatically at read time (see {@link PermissionSet}) — no re-pairing needed. Explicit
 *     grants are never expanded.</li>
 * <li>Coordinate with a Connect app release: apps without the tolerant pairing-code decoder
 *     (bisq-mobile, 2026-08) reject pairing codes that carry more permissions than they know,
 *     so new permissions break pairing for older apps.</li>
 * <li>Security-sensitive permissions (e.g. anything wallet/spend related) are never covered by
 *     the grantAll expansion — declare them with {@code autoGrantable = false} on the
 *     {@link Permission} enum and the exclusion is enforced by construction (see
 *     {@link PermissionSet}); they then always require an explicit per-device grant with
 *     deliberate approval.</li>
 * </ul>
 */
@Slf4j
public final class RestPermissionMapping implements PermissionMapping {
    private final List<PermissionRule> rules;

    public RestPermissionMapping() {
        // TODO apply rules to actual endpoints and methods. Atm we only check the root path
        this.rules = List.of(
                new PermissionRule("^/trade-chat-channels(/.*)?$", Optional.empty(), Permission.TRADE_CHAT_CHANNELS),
                new PermissionRule("^/private-chat-channels(/.*)?$", Optional.empty(), Permission.PRIVATE_CHAT_CHANNELS),
                new PermissionRule("^/explorer(/.*)?$", Optional.empty(), Permission.EXPLORER),
                new PermissionRule("^/market-price(/.*)?$", Optional.empty(), Permission.MARKET_PRICE),
                new PermissionRule("^/offerbook(/.*)?$", Optional.empty(), Permission.OFFERBOOK),
                new PermissionRule("^/payment-accounts(/.*)?$", Optional.empty(), Permission.PAYMENT_ACCOUNTS),
                new PermissionRule("^/reputation(/.*)?$", Optional.empty(), Permission.REPUTATION),
                new PermissionRule("^/trade-restricting-alert(/.*)?$", Optional.empty(), Permission.SETTINGS),
                new PermissionRule("^/alert-notifications(/.*)?$", Optional.empty(), Permission.SETTINGS),
                new PermissionRule("^/settings(/.*)?$", Optional.empty(), Permission.SETTINGS),
                new PermissionRule("^/trades(/.*)?$", Optional.empty(), Permission.TRADES),
                new PermissionRule("^/user-identities(/.*)?$", Optional.empty(), Permission.USER_IDENTITIES),
                new PermissionRule("^/user-profiles(/.*)?$", Optional.empty(), Permission.USER_PROFILES),
                new PermissionRule("^/mobile-devices(/.*)?$", Optional.empty(), Permission.MOBILE_DEVICES)
        );
    }

    @Override
    public Permission getRequiredPermission(String path, String method) {
        String normalizedPath = path.replace("/api/v1", "");
        return rules.stream()
                .filter(rule -> rule.matches(normalizedPath, method))
                .map(PermissionRule::permission)
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("Required permissions not granted"));
    }
}

