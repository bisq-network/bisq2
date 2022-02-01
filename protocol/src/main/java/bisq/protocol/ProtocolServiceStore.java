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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
public class ProtocolServiceStore implements PersistableStore<ProtocolServiceStore> {
    @Getter
    private final Map<String, ProtocolStore<?>> protocolStoreByOfferId = new ConcurrentHashMap<>();

    public ProtocolServiceStore() {
    }

    private ProtocolServiceStore(Map<String, ProtocolStore<?>> protocolStoreByOfferId) {
        this.protocolStoreByOfferId.putAll(protocolStoreByOfferId);
    }

    @Override
    public ProtocolServiceStore getClone() {
        return new ProtocolServiceStore(protocolStoreByOfferId);
    }

    @Override
    public void applyPersisted(ProtocolServiceStore persisted) {
        log.error("applyPersisted {}", persisted);
        protocolStoreByOfferId.clear();
        protocolStoreByOfferId.putAll(persisted.getProtocolStoreByOfferId());
    }

    public void add(Protocol<? extends ProtocolStore<?>> protocol) {
        if (protocolStoreByOfferId.containsKey(protocol.getId())) return;

        protocolStoreByOfferId.put(protocol.getId(), protocol.getPersistableStore());
    }

    public void remove(Protocol<? extends ProtocolStore<?>> protocol) {
        if (!protocolStoreByOfferId.containsKey(protocol.getId())) return;

        protocolStoreByOfferId.remove(protocol.getId());
    }
}