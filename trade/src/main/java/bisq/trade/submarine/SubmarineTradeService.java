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

package bisq.trade.submarine;

import bisq.common.application.Service;
import bisq.contract.submarine.SubmarineContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.offer.submarine.SubmarineOffer;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.trade.ServiceProvider;
import bisq.trade.protocol.TradeProtocol;
import bisq.trade.submarine.protocol.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public class SubmarineTradeService implements PersistenceClient<SubmarineTradeStore>, Service, ConfidentialMessageService.Listener {
    @Getter
    private final SubmarineTradeStore persistableStore = new SubmarineTradeStore();
    @Getter
    private final Persistence<SubmarineTradeStore> persistence;
    private final ServiceProvider serviceProvider;

    // We don't persist the protocol, only the model.
    private final Map<String, SubmarineProtocol> tradeProtocolById = new ConcurrentHashMap<>();

    public SubmarineTradeService(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        persistence = serviceProvider.getPersistenceService().getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
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

    public SubmarineTrade onTakeOffer(Identity takerIdentity, SubmarineOffer submarineOffer) {
        NetworkId takerNetworkId = takerIdentity.getNetworkId();
        SubmarineContract contract = new SubmarineContract(System.currentTimeMillis(),
                submarineOffer,
                takerNetworkId);
        boolean isBuyer = submarineOffer.getTakersDirection().isBuy();
        SubmarineTrade tradeModel = new SubmarineTrade(isBuyer, true, takerIdentity, contract, takerNetworkId);
        checkArgument(findProtocol(tradeModel.getId()).isPresent(),
                "We received the BisqEasyTakeOfferRequest for an already existing protocol");

        persistableStore.add(tradeModel);

        TradeProtocol<SubmarineTrade> tradeProtocol = createAndAddTradeProtocol(tradeModel);
        // protocol.handle(new SubmarineTakeOfferEvent(takerIdentity, contract));
        persist();
        return tradeModel;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Misc API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<SubmarineTrade> findTrade(String tradeId) {
        return persistableStore.findTrade(tradeId);
    }

    public Optional<SubmarineProtocol> findProtocol(String id) {
        return Optional.ofNullable(tradeProtocolById.get(id));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // TradeProtocol factory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public SubmarineProtocol createAndAddTradeProtocol(SubmarineTrade model) {
        String id = model.getId();
        SubmarineProtocol tradeProtocol;
        boolean isBuyer = model.isBuyer();
        if (model.isTaker()) {
            if (isBuyer) {
                tradeProtocol = new SubmarineBuyerAsTakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new SubmarineSellerAsTakerProtocol(serviceProvider, model);
            }
        } else {
            if (isBuyer) {
                tradeProtocol = new SubmarineBuyerAsMakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new SubmarineSellerAsMakerProtocol(serviceProvider, model);
            }
        }
        tradeProtocolById.put(id, tradeProtocol);
        return tradeProtocol;
    }
}