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
public class ProtocolStore implements PersistableStore<ProtocolStore> {
    @Getter
    private final Map<String, ProtocolModel> protocolModelByOfferId = new ConcurrentHashMap<>();
  
    public ProtocolStore() {
    }

    private ProtocolStore(Map<String, ProtocolModel> protocolModelByOfferId) {
        this.protocolModelByOfferId.putAll(protocolModelByOfferId);
    }

    @Override
    public ProtocolStore getClone() {
        return new ProtocolStore(protocolModelByOfferId);
    }

    @Override
    public void applyPersisted(ProtocolStore persisted) {
        protocolModelByOfferId.clear();
        protocolModelByOfferId.putAll(persisted.getProtocolModelByOfferId());
    }

    public void add( ProtocolModel protocolModel) {
        String protocolId = protocolModel.getId();
        if (!protocolModelByOfferId.containsKey(protocolId)) {
            protocolModelByOfferId.put(protocolId, protocolModel);
        }
    }

  /*  public void remove(Protocol<? extends ProtocolStore<?>> protocol) {
        String protocolId = protocol.getId();
        if (!protocolStoreByOfferId.containsKey(protocolId)) return;

        protocolsByOfferId.remove(protocolId);
        protocolStoreByOfferId.remove(protocolId);
    }*/
}