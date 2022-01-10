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
    SOCIAL,
    TRADE_INTENT(SOCIAL),
    HANGOUT(SOCIAL),
    OFFERBOOK,
    PORTFOLIO,
    WALLET,
    CREATE_OFFER(StageType.OVERLAY),
    SETTINGS,
    PREFERENCES(SETTINGS),
    ABOUT(SETTINGS),
    NETWORK_INFO(SETTINGS),
    CLEAR_NET(SETTINGS, NETWORK_INFO),
    TOR(SETTINGS, NETWORK_INFO),
    I2P(SETTINGS, NETWORK_INFO);

    @Getter
    private final StageType stageType;
    @Getter
    private final List<NavigationTarget> path = new ArrayList<>();

    NavigationTarget() {
        this(StageType.PRIMARY);
    }

    NavigationTarget(StageType stageType) {
        this(stageType, new NavigationTarget[]{});
    }

    NavigationTarget(NavigationTarget... path) {
        this(StageType.PRIMARY, path);
    }

    NavigationTarget(StageType stageType, NavigationTarget... path) {
        this.stageType = stageType;
        this.path.addAll(List.of(path));

        if (!this.path.isEmpty()) {
            checkArgument(this.path.get(0).getPath().isEmpty(),
                    "First element in path must point to a root NavigationTarget. " +
                            "NavigationTarget=" + this);
        }
    }
}