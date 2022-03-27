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

package bisq.network;

import bisq.persistence.PersistableStore;
import com.google.protobuf.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
//todo implement proto support after persistence is done
public class NetworkIdStore implements PersistableStore<NetworkIdStore> {
    @Getter
    private final Map<String, NetworkId> networkIdByNodeId = new ConcurrentHashMap<>();

    public NetworkIdStore() {
    }

    public NetworkIdStore(Map<String, NetworkId> networkIdByNodeId) {
        this.networkIdByNodeId.putAll(networkIdByNodeId);
    }

    @Override
    public void applyPersisted(NetworkIdStore persisted) {
        networkIdByNodeId.clear();
        networkIdByNodeId.putAll(persisted.getNetworkIdByNodeId());
    }

    @Override
    public NetworkIdStore getClone() {
        return new NetworkIdStore(networkIdByNodeId);
    }

    @Override
    public Message toProto() {
        log.error("Not impl yet");
        return null;
    }
}