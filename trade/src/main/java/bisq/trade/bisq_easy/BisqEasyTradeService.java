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

package bisq.trade.bisq_easy;

import bisq.common.application.Service;
import bisq.common.monetary.Monetary;
import bisq.contract.ContractService;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
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
import bisq.support.MediationService;
import bisq.support.SupportService;
import bisq.trade.Protocol;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.events.BisqEasyTakeOfferEvent;
import bisq.trade.bisq_easy.messages.BisqEasyTakeOfferRequest;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class BisqEasyTradeService implements PersistenceClient<BisqEasyTradeStore>, Service, MessageListener {
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

    // We don't persist the protocol, only the model.
    private final Map<String, BisqEasyProtocol> tradeProtocolById = new ConcurrentHashMap<>();

    public BisqEasyTradeService(NetworkService networkService,
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
        if (networkMessage instanceof BisqEasyTakeOfferRequest) {
            onBisqEasyTakeOfferMessage((BisqEasyTakeOfferRequest) networkMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Message event
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void onBisqEasyTakeOfferMessage(BisqEasyTakeOfferRequest message) {
        NetworkId takerNetworkId = message.getSender();
        BisqEasyContract bisqEasyContract = message.getBisqEasyContract();
        BisqEasyTrade tradeModel = new BisqEasyTrade(bisqEasyContract, takerNetworkId);
        persistableStore.add(tradeModel);
        persist();

        Protocol<BisqEasyTrade> protocol = findOrCreateTradeProtocol(tradeModel, false);
        try {
            protocol.handle(message);
        } catch (TradeException e) {
            log.error("Error at processing message " + message, e);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public BisqEasyTrade onTakeOffer(Identity takerIdentity,
                                     BisqEasyOffer bisqEasyOffer,
                                     Monetary baseSideAmount,
                                     Monetary quoteSideAmount,
                                     BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec,
                                     FiatPaymentMethodSpec fiatPaymentMethodSpec) throws TradeException {
        Optional<UserProfile> mediator = serviceProvider.getMediationService().takerSelectMediator(bisqEasyOffer.getMakersUserProfileId());
        NetworkId takerNetworkId = takerIdentity.getNetworkId();
        BisqEasyContract bisqEasyContract = new BisqEasyContract(bisqEasyOffer,
                takerNetworkId,
                baseSideAmount.getValue(),
                quoteSideAmount.getValue(),
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator);
        BisqEasyTrade tradeModel = new BisqEasyTrade(bisqEasyContract, takerNetworkId);
        Protocol<BisqEasyTrade> protocol = findOrCreateTradeProtocol(tradeModel, true);
        persistableStore.add(tradeModel);
        persist();

        protocol.handle(new BisqEasyTakeOfferEvent(takerIdentity, bisqEasyContract));

        return tradeModel;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<BisqEasyTrade> findBisqEasyTrade(String tradeId) {
        return persistableStore.findBisqEasyTrade(tradeId);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // TradeProtocol factory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public BisqEasyProtocol findOrCreateTradeProtocol(BisqEasyTrade model, boolean isTaker) {
        String id = model.getId();
        if (tradeProtocolById.containsKey(id)) {
            return tradeProtocolById.get(id);
        }

        BisqEasyProtocol tradeProtocol;
        if (isTaker) {
            boolean isBuyer = model.getOffer().getTakersDirection().isBuy();
            if (isBuyer) {
                tradeProtocol = new BisqEasyBuyerAsTakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new BisqEasySellerAsTakerProtocol(serviceProvider, model);
            }
        } else {
            boolean isBuyer = model.getOffer().getMakersDirection().isBuy();
            if (isBuyer) {
                tradeProtocol = new BisqEasyBuyerAsMakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new BisqEasySellerAsMakerProtocol(serviceProvider, model);
            }
        }
        tradeProtocolById.putIfAbsent(id, tradeProtocol);
        return tradeProtocol;
    }
}