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

package bisq.protocol;

import bisq.persistence.PersistableStore;
import bisq.protocol.reputation.Protocol;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolStore implements PersistableStore<ProtocolStore> {
    @Getter
    private final Map<String, Protocol> protocolByOfferId = new ConcurrentHashMap<>();

    public ProtocolStore() {
    }

    private ProtocolStore(Map<String, Protocol> protocolByOfferId) {
        this.protocolByOfferId.putAll(protocolByOfferId);
    }

    @Override
    public ProtocolStore getClone() {
        return new ProtocolStore(protocolByOfferId);
    }

    @Override
    public void applyPersisted(ProtocolStore persisted) {
        protocolByOfferId.clear();
        protocolByOfferId.putAll(persisted.getProtocolByOfferId());
    }

    public void add(Protocol protocol) {
        if (protocolByOfferId.containsKey(protocol.getId())) return;

        protocolByOfferId.put(protocol.getId(), protocol);
    }

    public void remove(Protocol protocol) {
        if (!protocolByOfferId.containsKey(protocol.getId())) return;

        protocolByOfferId.remove(protocol.getId());
    }
}