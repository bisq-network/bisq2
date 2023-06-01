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

package bisq.protocol.poc;

import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceClient;

public abstract class TakerPocProtocol<T extends PocProtocolModel> extends PocProtocol<T> {
    public TakerPocProtocol(NetworkService networkService,
                            PersistenceClient<PocProtocolStore> persistenceClient,
                            T protocolModel,
                            NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        super(networkService,
                persistenceClient,
                protocolModel,
                myNodeIdAndKeyPair);
    }

    @Override
    protected NetworkId getPeersNetworkId() {
        return getContract().getOffer().getMakerNetworkId();
    }

    public abstract void takeOffer();
}