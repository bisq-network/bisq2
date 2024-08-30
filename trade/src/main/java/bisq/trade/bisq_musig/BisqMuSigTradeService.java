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

package bisq.trade.bisq_musig;

import bisq.common.application.Service;
import bisq.contract.bisq_musig.BisqMuSigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.offer.bisq_musig.BisqMuSigOffer;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.protocol.*;
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
public class BisqMuSigTradeService implements PersistenceClient<BisqMuSigTradeStore>, Service, ConfidentialMessageService.Listener {
    @Getter
    private final BisqMuSigTradeStore persistableStore = new BisqMuSigTradeStore();
    @Getter
    private final Persistence<BisqMuSigTradeStore> persistence;
    private final ServiceProvider serviceProvider;

    // We don't persist the protocol, only the model.
    private final Map<String, BisqMuSigProtocol> tradeProtocolById = new ConcurrentHashMap<>();

    public BisqMuSigTradeService(ServiceProvider serviceProvider) {
        persistence = serviceProvider.getPersistenceService().getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.serviceProvider = serviceProvider;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        serviceProvider.getNetworkService().addConfidentialMessageListener(this);

        persistableStore.getTradeById().values().forEach(this::createAndAddTradeProtocol);

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        serviceProvider.getNetworkService().removeConfidentialMessageListener(this);
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

    public BisqMuSigTrade onTakeOffer(Identity takerIdentity, BisqMuSigOffer bisqMuSigOffer) {
        NetworkId takerNetworkId = takerIdentity.getNetworkId();
        BisqMuSigContract contract = new BisqMuSigContract(System.currentTimeMillis(),
                bisqMuSigOffer,
                takerNetworkId);
        boolean isBuyer = bisqMuSigOffer.getTakersDirection().isBuy();
        BisqMuSigTrade tradeModel = new BisqMuSigTrade(isBuyer, true, takerIdentity, contract, takerNetworkId);
        checkArgument(findProtocol(tradeModel.getId()).isPresent(),
                "We received the TakeOfferRequest for an already existing protocol");

        persistableStore.add(tradeModel);

        TradeProtocol<BisqMuSigTrade> tradeProtocol = createAndAddTradeProtocol(tradeModel);
        // protocol.handle(new BisqMuSigTakeOfferEvent(takerIdentity, contract));
        persist();
        return tradeModel;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Misc API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<BisqMuSigTrade> findTrade(String tradeId) {
        return persistableStore.findTrade(tradeId);
    }

    public Optional<BisqMuSigProtocol> findProtocol(String id) {
        return Optional.ofNullable(tradeProtocolById.get(id));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // TradeProtocol factory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public BisqMuSigProtocol createAndAddTradeProtocol(BisqMuSigTrade model) {
        String id = model.getId();
        BisqMuSigProtocol tradeProtocol;
        boolean isBuyer = model.isBuyer();
        if (model.isTaker()) {
            if (isBuyer) {
                tradeProtocol = new BisqMuSigBuyerAsTakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new BisqMuSigSellerAsTakerProtocol(serviceProvider, model);
            }
        } else {
            if (isBuyer) {
                tradeProtocol = new BisqMuSigBuyerAsMakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new BisqMuSigSellerAsMakerProtocol(serviceProvider, model);
            }
        }
        tradeProtocolById.put(id, tradeProtocol);
        return tradeProtocol;
    }
}