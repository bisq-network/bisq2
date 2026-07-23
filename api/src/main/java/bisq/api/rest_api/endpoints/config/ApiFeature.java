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

package bisq.api.rest_api.endpoints.config;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

/**
 * Registry of the "recent" API features a client may gate its UI on — capabilities that older nodes
 * (or a Bisq Desktop node) might not have yet. Served as stable kebab-case keys by {@code ConfigRestApi}.
 * <p>
 * ONLY list features that are genuinely new enough that some reachable nodes lack them. Long-standing,
 * universally-available behaviour does not belong here — the list should stay small. Add a key ONLY once
 * the feature is actually implemented in this build; {@code ConfigRestApiTest} fails otherwise.
 * <p>
 * Example — closed trades: a client gates its "Closed trades" screen on {@link #CLOSED_TRADES}. A node
 * that lists it exposes {@code GET /trades/closed}; a node without the capabilities endpoint returns 404,
 * so the client reads it as unsupported and hides the screen. New features (e.g. a future
 * {@code "network-info"} once its subscription topic ships) are added the same way.
 */
@Getter
public enum ApiFeature {
    CLOSED_TRADES("closed-trades");

    private final String key;

    ApiFeature(String key) {
        this.key = key;
    }

    public static List<String> allKeys() {
        return Arrays.stream(values()).map(ApiFeature::getKey).toList();
    }
}
