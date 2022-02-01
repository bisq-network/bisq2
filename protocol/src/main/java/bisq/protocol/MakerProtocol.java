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

import bisq.contract.Contract;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.protocol.messages.TakeOfferRequest;

public abstract class MakerProtocol<T extends ProtocolStore<T>, R extends TakeOfferRequest> extends Protocol<T> {

    public MakerProtocol(NetworkService networkService,
                         PersistenceService persistenceService,
                         Contract contract,
                         NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        super(networkService,
                persistenceService,
                contract,
                myNodeIdAndKeyPair);
    }

    @Override
    protected NetworkId getPeersNetworkId() {
        return getContract().getTakerNetworkId();
    }

    public void onRawTakeOfferRequest(TakeOfferRequest takeOfferRequest) {
        onTakeOfferRequest(castTakeOfferRequest(takeOfferRequest));
    }

    protected abstract R castTakeOfferRequest(TakeOfferRequest takeOfferRequest);

    abstract public void onTakeOfferRequest(R takeOfferRequest);
}