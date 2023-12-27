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

package bisq.network.p2p.node.network_load;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class NetworkLoadSnapshot {
    private NetworkLoad currentNetworkLoad;
    @Nullable
    private NetworkLoad previousNetworkLoad = null;
    private long lastUpdated = 0;

    public NetworkLoadSnapshot() {
        currentNetworkLoad = new NetworkLoad();
    }

    public NetworkLoadSnapshot(NetworkLoad networkLoad) {
        currentNetworkLoad = networkLoad;
    }

    public void updateNetworkLoad(NetworkLoad networkLoad) {
        synchronized (this) {
            lastUpdated = System.currentTimeMillis();
            previousNetworkLoad = currentNetworkLoad;
            currentNetworkLoad = networkLoad;
        }
    }
}