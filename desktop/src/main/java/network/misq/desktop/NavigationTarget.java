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

package network.misq.desktop;

import lombok.Getter;

public enum NavigationTarget {
    MARKETS,
    OFFERBOOK,
    CREATE_OFFER(NavigationSink.OVERLAY),
    SETTINGS,
    NETWORK_INFO;
    @Getter
    private final NavigationSink sink;

    NavigationTarget() {
        this.sink = NavigationSink.CONTENT;
    }

    NavigationTarget(NavigationSink sink) {
        this.sink = sink;
    }
}