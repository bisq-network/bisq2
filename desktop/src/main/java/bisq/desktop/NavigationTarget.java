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

package bisq.desktop;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public enum NavigationTarget {
    MARKETS,
    OFFERBOOK,
    CREATE_OFFER(NavigationSink.OVERLAY),
    SETTINGS,
    PREFERENCES(SETTINGS),
    ABOUT(SETTINGS),
    NETWORK_INFO(SETTINGS),
    TRANSPORT_TYPE(SETTINGS, NETWORK_INFO);


    @Getter
    private final NavigationSink sink;
    @Getter
    private final List<NavigationTarget> path = new ArrayList<>();

    NavigationTarget() {
        this(NavigationSink.CONTENT);
    }

    NavigationTarget(NavigationSink sink) {
        this(sink, new NavigationTarget[]{});
    }

    NavigationTarget(NavigationTarget... path) {
        this(NavigationSink.CONTENT, path);
    }

    NavigationTarget(NavigationSink sink, NavigationTarget... path) {
        this.sink = sink;
        this.path.addAll(List.of(path));

        if (!this.path.isEmpty()) {
            checkArgument(this.path.get(0).getPath().isEmpty(),
                    "First element in path must point to a root NavigationTarget. " +
                            "NavigationTarget=" + this);
        }
    }
}