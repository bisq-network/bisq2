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

package bisq.protocol.reputation;

import bisq.contract.Contract;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.persistence.PersistenceService;
import bisq.protocol.reputation.messages.TakeOfferRequest;
import bisq.protocol.reputation.messages.taker.RP_TakeOfferRequest;

public class RP_MakerProtocol extends Protocol {

    public RP_MakerProtocol(NetworkService networkService,
                            PersistenceService persistenceService,
                            Contract contract,
                            NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        super(networkService,
                persistenceService,
                contract,
                myNodeIdAndKeyPair);
    }

    private void onTakeOfferRequest(RP_TakeOfferRequest takeOfferRequest) {
    }

    @Override
    public void onMessage(Message message) {

    }

    @Override
    public void onTakeOfferRequest(TakeOfferRequest takeOfferRequest) {
        if (takeOfferRequest instanceof RP_TakeOfferRequest request) {
            onTakeOfferRequest(request);
        }else{
            //todo error
        }
    }
}