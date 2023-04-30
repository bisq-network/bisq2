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

import bisq.account.protocol.SwapProtocolType;
import bisq.common.application.Service;
import bisq.common.monetary.Monetary;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.contract.Contract;
import bisq.identity.IdentityService;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.offer.Offer;
import bisq.offer.OpenOfferService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.protocol.liquidswap.LiquidSwapMakerProtocol;
import bisq.protocol.liquidswap.LiquidSwapTakerProtocol;
import bisq.protocol.messages.TakeOfferRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class ProtocolService implements MessageListener, PersistenceClient<ProtocolStore>, Service {
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("ProtocolService.dispatcher");

    @Getter
    private final ProtocolStore persistableStore = new ProtocolStore();
    @Getter
    private final Persistence<ProtocolStore> persistence;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final OpenOfferService openOfferService;
    @Getter
    private final ObservableSet<Protocol<? extends ProtocolModel>> protocols = new ObservableSet<>();

    public ProtocolService(NetworkService networkService,
                           IdentityService identityService,
                           PersistenceService persistenceService,
                           OpenOfferService openOfferService) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.openOfferService = openOfferService;

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        networkService.addMessageListener(this);
    }

    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFutureUtils.allOf(persistableStore.getProtocolModelByOfferId().values().stream()
                        .map(protocolModel ->
                                identityService.getOrCreateIdentity(protocolModel.getId())
                                        .thenApply(identity -> {
                                            Protocol<? extends ProtocolModel> protocol;
                                            if (protocolModel instanceof MakerProtocolModel) {
                                                protocol = getMakerProtocol((MakerProtocolModel) protocolModel, identity.getNodeIdAndKeyPair());
                                            } else if (protocolModel instanceof TakerProtocolModel) {
                                                protocol = getTakerProtocol((TakerProtocolModel) protocolModel, identity.getNodeIdAndKeyPair());
                                            } else {
                                                return false;
                                            }
                                   /* // We do not rely on equals and hash code as protocol is very content rich. 
                                    // We just want to be sure to always have the latest version, so if we find 
                                    // one with the same id, we replace it with the new one.
                                    // We could use a map instead but atm we don't have an ObservedSet-like implementation for a map (todo).
                                    Optional<Protocol<? extends ProtocolModel>> optionalProtocol = protocols.stream()
                                            .filter(p -> p.getId().equals(protocol.getId()))
                                            .findAny();
                                    optionalProtocol.ifPresent(protocols::remove);*/

                                            protocols.add(protocol);
                                            persistableStore.add(protocolModel);
                                            if (protocol.isPending()) {
                                                protocol.onContinue();
                                            }
                                            return true;
                                        })
                        ))
                .thenApply(list -> true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof TakeOfferRequest) {
            TakeOfferRequest takeOfferRequest = (TakeOfferRequest) networkMessage;
            String offerId = takeOfferRequest.getContract().getOffer().getId();
            openOfferService.findOpenOffer(offerId)
                    .ifPresent(openOffer -> identityService.getOrCreateIdentity(offerId)
                            .whenComplete((identity, throwable) -> {
                                MakerProtocolModel protocolModel = new MakerProtocolModel(takeOfferRequest.getContract());
                                var protocol = getMakerProtocol(protocolModel, identity.getNodeIdAndKeyPair());
                                persistableStore.add(protocolModel);
                                protocols.add(protocol);
                                persist();

                                //todo figure out how to use generics without that hack
                                protocol.onRawTakeOfferRequest(takeOfferRequest);
                            }));
        }
    }

    public CompletableFuture<TakerProtocol<TakerProtocolModel>> takeOffer(SwapProtocolType protocolType,
                                                                          Offer offer,
                                                                          Monetary baseSideAmount,
                                                                          Monetary quoteSideAmount,
                                                                          String baseSideSettlementMethod,
                                                                          String quoteSideSettlementMethod) {
        return identityService.getOrCreateIdentity(offer.getId())
                .thenApply(identity -> {
                    Contract contract = new Contract(identity.getNetworkId(),
                            protocolType,
                            offer,
                            baseSideAmount,
                            quoteSideAmount,
                            baseSideSettlementMethod,
                            quoteSideSettlementMethod);
                    TakerProtocolModel protocolModel = new TakerProtocolModel(contract);
                    TakerProtocol<TakerProtocolModel> protocol = getTakerProtocol(protocolModel, identity.getNodeIdAndKeyPair());
                    persistableStore.add(protocolModel);
                    protocols.add(protocol);
                    persist();
                    protocol.takeOffer();
                    return protocol;
                });
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private TakerProtocol<TakerProtocolModel> getTakerProtocol(TakerProtocolModel protocolModel,
                                                               NetworkIdWithKeyPair takerNodeIdAndKeyPair) {
        switch (protocolModel.getContract().getProtocolType()) {
            case MONERO_SWAP:
                return null;
            case LIQUID_SWAP:
                return LiquidSwapTakerProtocol.getProtocol(networkService, this, protocolModel, takerNodeIdAndKeyPair);
            case BSQ_SWAP:
                return null;
            case LIGHTNING_X:
                return null;
            case BISQ_MULTISIG:
                return null;
        }
        return null;
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private MakerProtocol<MakerProtocolModel, ? extends TakeOfferRequest> getMakerProtocol(MakerProtocolModel protocolModel,
                                                                                           NetworkIdWithKeyPair makerNetworkIdWithKeyPair) {
        switch (protocolModel.getContract().getProtocolType()) {
            case MONERO_SWAP:
                return null;
            case LIQUID_SWAP:
                return LiquidSwapMakerProtocol.getProtocol(networkService, this, protocolModel, makerNetworkIdWithKeyPair);
            case BSQ_SWAP:
                return null;
            case LIGHTNING_X:
                return null;
            case BISQ_MULTISIG:
                return null;
        }
        return null;
    }
}