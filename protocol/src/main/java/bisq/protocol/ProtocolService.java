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
import bisq.common.monetary.Monetary;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.contract.Contract;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class ProtocolService implements MessageListener, PersistenceClient<ProtocolServiceStore> {
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("NetworkService.dispatcher");

    @Getter
    private final ProtocolServiceStore persistableStore = new ProtocolServiceStore();
    @Getter
    private final Persistence<ProtocolServiceStore> persistence;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final PersistenceService persistenceService;
    private final OpenOfferService openOfferService;

    public ProtocolService(NetworkService networkService,
                           IdentityService identityService,
                           PersistenceService persistenceService,
                           OpenOfferService openOfferService) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.persistenceService = persistenceService;
        this.openOfferService = openOfferService;

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        networkService.addMessageListener(this);
    }

    public CompletableFuture<List<Boolean>> initialize() {
        return CompletableFutureUtils.allOf(persistableStore.getProtocolStoreByOfferId().values().stream()
                .filter(ProtocolStore::isPending)
                .map(protocolStore ->
                        identityService.getOrCreateIdentity(protocolStore.getId())
                                .thenApply(identity -> {
                                    if (protocolStore instanceof MakerProtocolStore) {
                                        var protocol = getMakerProtocol(protocolStore.getContract(), identity.getNodeIdAndKeyPair());
                                        protocol.onContinue();
                                        return true;
                                    } else if (protocolStore instanceof TakerProtocolStore) {
                                        TakerProtocol<TakerProtocolStore> protocol = getTakerProtocol(protocolStore.getContract(), identity.getNodeIdAndKeyPair());
                                        protocol.onContinue();
                                        return true;
                                    } else {
                                        return false;
                                    }
                                })
                ));
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof TakeOfferRequest takeOfferRequest) {
            String offerId = takeOfferRequest.getContract().getOffer().getId();
            openOfferService.findOpenOffer(offerId)
                    .ifPresent(openOffer -> identityService.getOrCreateIdentity(offerId)
                            .whenComplete((identity, throwable) -> {
                                var protocol = getMakerProtocol(takeOfferRequest.getContract(), identity.getNodeIdAndKeyPair());
                                persistableStore.add(protocol);
                                persist();

                                //todo figure out how to use generics without that hack
                                protocol.onRawTakeOfferRequest(takeOfferRequest);
                            }));
        }
    }

    public CompletableFuture<Identity> takeOffer(SwapProtocolType protocolType,
                                                 Offer offer,
                                                 Monetary baseSideAmount,
                                                 Monetary quoteSideAmount,
                                                 String baseSideSettlementMethod,
                                                 String quoteSideSettlementMethod) {
        return identityService.getOrCreateIdentity(offer.getId())
                .whenComplete((identity, throwable) -> {
                    Contract contract = new Contract(identity.networkId(),
                            protocolType,
                            offer,
                            baseSideAmount,
                            quoteSideAmount,
                            baseSideSettlementMethod,
                            quoteSideSettlementMethod);
                    TakerProtocol<TakerProtocolStore> protocol = getTakerProtocol(contract, identity.getNodeIdAndKeyPair());
                    persistableStore.add(protocol);
                    persist();
                    protocol.takeOffer();
                });
    }

    private TakerProtocol<TakerProtocolStore> getTakerProtocol(Contract contract, NetworkIdWithKeyPair takerNodeIdAndKeyPair) {
        return switch (contract.getProtocolType()) {
            case BTC_XMR_SWAP -> null;
            case LIQUID_SWAP -> LiquidSwapTakerProtocol.getProtocol(networkService, persistenceService, contract, takerNodeIdAndKeyPair);
            case BSQ_SWAP -> null;
            case LN_SWAP -> null;
            case MULTISIG -> null;
            case BSQ_BOND -> null;
            case REPUTATION -> null;
        };
    }

    private MakerProtocol<MakerProtocolStore, ? extends TakeOfferRequest> getMakerProtocol(Contract contract, NetworkIdWithKeyPair makerNetworkIdWithKeyPair) {
        return switch (contract.getProtocolType()) {
            case BTC_XMR_SWAP -> null;
            case LIQUID_SWAP -> LiquidSwapMakerProtocol.getProtocol(networkService, persistenceService, contract, makerNetworkIdWithKeyPair);
            case BSQ_SWAP -> null;
            case LN_SWAP -> null;
            case MULTISIG -> null;
            case BSQ_BOND -> null;
            case REPUTATION -> null;
        };
    }
}