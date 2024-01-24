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

package bisq.trade.multisig;

import bisq.common.application.Service;
import bisq.contract.multisig.MultisigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.offer.multisig.MultisigOffer;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.trade.ServiceProvider;
import bisq.trade.TradeProtocolException;
import bisq.trade.multisig.protocol.*;
import bisq.trade.protocol.TradeProtocol;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public class MultisigTradeService implements PersistenceClient<MultisigTradeStore>, Service, MessageListener {
    @Getter
    private final MultisigTradeStore persistableStore = new MultisigTradeStore();
    @Getter
    private final Persistence<MultisigTradeStore> persistence;
    private final ServiceProvider serviceProvider;

    // We don't persist the protocol, only the model.
    private final Map<String, MultisigProtocol> tradeProtocolById = new ConcurrentHashMap<>();

    public MultisigTradeService(ServiceProvider serviceProvider) {
        persistence = serviceProvider.getPersistenceService().getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.serviceProvider = serviceProvider;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        serviceProvider.getNetworkService().addMessageListener(this);

        persistableStore.getTradeById().values().forEach(this::createAndAddTradeProtocol);

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        serviceProvider.getNetworkService().removeMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Message event
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public MultisigTrade onTakeOffer(Identity takerIdentity,
                                     MultisigOffer multisigOffer) throws TradeProtocolException {
        NetworkId takerNetworkId = takerIdentity.getNetworkId();
        MultisigContract contract = new MultisigContract(System.currentTimeMillis(),
                multisigOffer,
                takerNetworkId);
        boolean isBuyer = multisigOffer.getTakersDirection().isBuy();
        MultisigTrade tradeModel = new MultisigTrade(isBuyer, true, takerIdentity, contract, takerNetworkId);
        checkArgument(findProtocol(tradeModel.getId()).isPresent(),
                "We received the TakeOfferRequest for an already existing protocol");

        persistableStore.add(tradeModel);

        TradeProtocol<MultisigTrade> tradeProtocol = createAndAddTradeProtocol(tradeModel);
        // protocol.handle(new MultisigTakeOfferEvent(takerIdentity, contract));
        persist();
        return tradeModel;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Misc API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<MultisigTrade> findTrade(String tradeId) {
        return persistableStore.findTrade(tradeId);
    }

    public Optional<MultisigProtocol> findProtocol(String id) {
        return Optional.ofNullable(tradeProtocolById.get(id));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // TradeProtocol factory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public MultisigProtocol createAndAddTradeProtocol(MultisigTrade model) {
        String id = model.getId();
        MultisigProtocol tradeProtocol;
        boolean isBuyer = model.isBuyer();
        if (model.isTaker()) {
            if (isBuyer) {
                tradeProtocol = new MultisigBuyerAsTakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new MultisigSellerAsTakerProtocol(serviceProvider, model);
            }
        } else {
            if (isBuyer) {
                tradeProtocol = new MultisigBuyerAsMakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new MultisigSellerAsMakerProtocol(serviceProvider, model);
            }
        }
        tradeProtocolById.put(id, tradeProtocol);
        return tradeProtocol;
    }
}