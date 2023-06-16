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

package bisq.protocol.bisq_easy;

import bisq.common.application.Service;
import bisq.common.monetary.Monetary;
import bisq.contract.ContractService;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.offer.OfferService;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.protocol.bisq_easy.buyer_as_maker.BisqEasyBuyerAsMakerProtocol;
import bisq.protocol.bisq_easy.buyer_as_maker.BisqEasyBuyerAsMakerTrade;
import bisq.protocol.bisq_easy.buyer_as_taker.BisqEasyBuyerAsTakerProtocol;
import bisq.protocol.bisq_easy.buyer_as_taker.BisqEasyBuyerAsTakerTrade;
import bisq.protocol.bisq_easy.maker.BisqEasyMakerProtocol;
import bisq.protocol.bisq_easy.messages.BisqEasyProtocolMessage;
import bisq.protocol.bisq_easy.seller_as_maker.BisqEasySellerAsMakerProtocol;
import bisq.protocol.bisq_easy.seller_as_maker.BisqEasySellerAsMakerTrade;
import bisq.protocol.bisq_easy.seller_as_taker.BisqEasySellerAsTakerProtocol;
import bisq.protocol.bisq_easy.seller_as_taker.BisqEasySellerAsTakerTrade;
import bisq.protocol.bisq_easy.taker.BisqEasyTakerProtocol;
import bisq.protocol.bisq_easy.taker.BisqEasyTakerTrade;
import bisq.protocol.bisq_easy.taker.messages.BisqEasyTakeOfferRequest;
import bisq.support.MediationService;
import bisq.support.SupportService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class BisqEasyProtocolService implements PersistenceClient<BisqEasyTradeStore>, Service, MessageListener {
    @Getter
    private final BisqEasyTradeStore persistableStore = new BisqEasyTradeStore();
    @Getter
    private final Persistence<BisqEasyTradeStore> persistence;
    private final IdentityService identityService;
    private final OfferService offerService;
    private final ContractService contractService;
    private final MediationService mediationService;
    private final NetworkService networkService;
    private final ServiceProvider serviceProvider;

    public BisqEasyProtocolService(NetworkService networkService,
                                   IdentityService identityService,
                                   PersistenceService persistenceService,
                                   OfferService offerService,
                                   ContractService contractService,
                                   SupportService supportService) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.offerService = offerService;
        this.contractService = contractService;
        this.mediationService = supportService.getMediationService();
        serviceProvider = new ServiceProvider(networkService,
                identityService,
                persistenceService,
                offerService,
                contractService,
                supportService);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        networkService.addMessageListener(this);
        return CompletableFuture.completedFuture(true);

    }

    public CompletableFuture<Boolean> shutdown() {
        networkService.removeMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof BisqEasyProtocolMessage) {
            processMessage((BisqEasyProtocolMessage) networkMessage);
        }
    }

    private void processMessage(BisqEasyProtocolMessage message) {
        if (message instanceof BisqEasyTakeOfferRequest) {
            onBisqEasyTakeOfferMessage((BisqEasyTakeOfferRequest) message);
        }
    }

    private void onBisqEasyTakeOfferMessage(BisqEasyTakeOfferRequest message) {
        BisqEasyOffer bisqEasyOffer = message.getBisqEasyContract().getOffer();
        boolean isBuyer = bisqEasyOffer.getTakersDirection().isBuy();
        BisqEasyProtocol<?> bisqEasyProtocol = createProtocol(isBuyer, false);
        BisqEasyTrade<?, ?> bisqEasyTrade = createTrade(bisqEasyProtocol);
        persistableStore.add(bisqEasyTrade);
        persist();
        BisqEasyMakerProtocol<?> bisqEasyTakerProtocol = (BisqEasyMakerProtocol<?>) bisqEasyProtocol;
        bisqEasyTakerProtocol.handleTakeOfferRequest(serviceProvider, message);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public BisqEasyTakerTrade<?, ?> takeOffer(Identity takerIdentity,
                                              BisqEasyOffer bisqEasyOffer,
                                              Monetary baseSideAmount,
                                              Monetary quoteSideAmount,
                                              BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec,
                                              FiatPaymentMethodSpec fiatPaymentMethodSpec) {
        boolean isBuyer = bisqEasyOffer.getTakersDirection().isBuy();
        BisqEasyProtocol<?> bisqEasyProtocol = createProtocol(isBuyer, true);
        BisqEasyTrade<?, ?> bisqEasyTrade = createTrade(bisqEasyProtocol);
        persistableStore.add(bisqEasyTrade);
        persist();
        BisqEasyTakerProtocol<?> bisqEasyTakerProtocol = (BisqEasyTakerProtocol<?>) bisqEasyProtocol;
        bisqEasyTakerProtocol.takeOffer(serviceProvider,
                takerIdentity,
                bisqEasyOffer,
                baseSideAmount,
                quoteSideAmount,
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec);
        return (BisqEasyTakerTrade<?, ?>) bisqEasyTrade;
    }

    public Optional<BisqEasyTrade<?, ?>> findBisqEasyTrade(String tradeId) {
        return persistableStore.findBisqEasyTrade(tradeId);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private static BisqEasyProtocol<?> createProtocol(boolean isBuyer, boolean isTaker) {
        if (isTaker) {
            if (isBuyer) {
                return new BisqEasyBuyerAsTakerProtocol(new BisqEasyProtocolModel());
            } else {
                return new BisqEasySellerAsTakerProtocol(new BisqEasyProtocolModel());
            }
        } else {
            if (isBuyer) {
                return new BisqEasyBuyerAsMakerProtocol(new BisqEasyProtocolModel());
            } else {
                return new BisqEasySellerAsMakerProtocol(new BisqEasyProtocolModel());
            }
        }
    }

    private static BisqEasyTrade<?, ?> createTrade(BisqEasyProtocol<?> protocol) {
        if (protocol instanceof BisqEasyBuyerAsTakerProtocol) {
            return new BisqEasyBuyerAsTakerTrade((BisqEasyBuyerAsTakerProtocol) protocol);
        } else if (protocol instanceof BisqEasyBuyerAsMakerProtocol) {
            return new BisqEasyBuyerAsMakerTrade((BisqEasyBuyerAsMakerProtocol) protocol);
        } else if (protocol instanceof BisqEasySellerAsTakerProtocol) {
            return new BisqEasySellerAsTakerTrade((BisqEasySellerAsTakerProtocol) protocol);
        } else if (protocol instanceof BisqEasySellerAsMakerProtocol) {
            return new BisqEasySellerAsMakerTrade((BisqEasySellerAsMakerProtocol) protocol);
        } else {
            throw new RuntimeException("Not handled protocol type. Protocol=" + protocol);
        }
    }
}