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

package network.misq.network.p2p;

import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

// TODO just for dev test setup
@Deprecated
public class MockNetworkService implements INetworkService {
    public interface Listener {
        void onDataAdded(Serializable serializable);

        void onDataRemoved(Serializable serializable);
    }

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    @Getter
    private final Map<String, Serializable> map = new HashMap<>();

    public MockNetworkService() {
    }

    public void initialize() {

    }

    public void addData(Serializable serializable) {
        map.put(serializable.toString(), serializable);
        listeners.forEach(e -> e.onDataAdded(serializable));
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
