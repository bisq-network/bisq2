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
import bisq.protocol.reputation.RP_MakerProtocol;
import bisq.protocol.reputation.RP_TakerProtocol;
import bisq.protocol.reputation.messages.TakeOfferRequest;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ProtocolService implements MessageListener, PersistenceClient<ProtocolStore> {
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("NetworkService.dispatcher");

    @Getter
    private final ProtocolStore persistableStore = new ProtocolStore();
    @Getter
    private final Persistence<ProtocolStore> persistence;
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

    @Override
    public void onMessage(Message message) {
        if (message instanceof TakeOfferRequest takeOfferRequest) {
            String offerId = takeOfferRequest.getContract().getOffer().getId();
            openOfferService.findOpenOffer(offerId)
                    .ifPresent(openOffer -> identityService.getOrCreateIdentity(offerId)
                            .whenComplete((identity, throwable) -> {
                                RP_MakerProtocol protocol = getMakerProtocol(takeOfferRequest.getContract(), identity.getNodeIdAndKeyPair());
                                persistableStore.add(protocol);
                                persist();
                                protocol.onTakeOfferRequest(takeOfferRequest);
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
                    RP_TakerProtocol protocol = getTakerProtocol(contract, identity.getNodeIdAndKeyPair());
                    persistableStore.add(protocol);
                    persist();
                    protocol.takeOffer();
                });
    }

    private RP_TakerProtocol getTakerProtocol(Contract contract, NetworkIdWithKeyPair takerNodeIdAndKeyPair) {
        return switch (contract.getProtocolType()) {
            case BTC_XMR_SWAP -> null;
            case LIQUID_SWAP -> null;
            case BSQ_SWAP -> null;
            case LN_SWAP -> null;
            case MULTISIG -> null;
            case BSQ_BOND -> null;
            case REPUTATION -> new RP_TakerProtocol(networkService, persistenceService, contract, takerNodeIdAndKeyPair);
        };
    }

    private RP_MakerProtocol getMakerProtocol(Contract contract, NetworkIdWithKeyPair makerNetworkIdWithKeyPair) {
        return switch (contract.getProtocolType()) {
            case BTC_XMR_SWAP -> null;
            case LIQUID_SWAP -> null;
            case BSQ_SWAP -> null;
            case LN_SWAP -> null;
            case MULTISIG -> null;
            case BSQ_BOND -> null;
            case REPUTATION -> new RP_MakerProtocol(networkService, persistenceService, contract, makerNetworkIdWithKeyPair);
        };
    }
}