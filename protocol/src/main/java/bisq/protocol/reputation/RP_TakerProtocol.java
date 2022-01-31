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
import bisq.protocol.reputation.messages.maker.RP_TakeOfferResponse;
import bisq.protocol.reputation.messages.taker.RP_TakeOfferRequest;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

public class RP_TakerProtocol extends Protocol {

    enum TakerState implements Protocol.State {
        INIT,
        SEND_TAKE_OFFER_REQUEST,
        TAKE_OFFER_REQUEST_ARRIVED,
        TAKE_OFFER_REQUEST_FAILED
    }


    private AtomicReference<TakerState> state = new AtomicReference<>(TakerState.INIT);
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    @Override
    public void onTakeOfferRequest(TakeOfferRequest takeOfferRequest) {
        
    }

    public RP_TakerProtocol(NetworkService networkService,
                            PersistenceService persistenceService,
                            Contract contract,
                            NetworkIdWithKeyPair myNodeIdAndKeyPair) {
        super(networkService,
                persistenceService,
                contract,
                myNodeIdAndKeyPair);
    }

    public void takeOffer() {
        RP_TakeOfferRequest takeOfferRequest = new RP_TakeOfferRequest(contract);
        setState(TakerState.SEND_TAKE_OFFER_REQUEST);
        networkService.sendMessage(takeOfferRequest, contract.getOffer().getMakerNetworkId(), myNodeIdAndKeyPair)
                .whenComplete((resultMap, throwable) -> {
                    if (throwable == null) {
                        setState(TakerState.TAKE_OFFER_REQUEST_ARRIVED);

                    } else {
                        setState(TakerState.TAKE_OFFER_REQUEST_FAILED);
                    }
                });
    }

    private void onTakeOfferResponse(RP_TakeOfferResponse RPTakeOfferResponse) {

    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof RP_TakeOfferResponse RPTakeOfferResponse) {
            onTakeOfferResponse(RPTakeOfferResponse);
        }
    }
}