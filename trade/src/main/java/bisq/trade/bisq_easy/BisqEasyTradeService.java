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
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.NetworkId;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.trade.ServiceProvider;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.protocol.*;
import bisq.trade.bisq_easy.protocol.events.*;
import bisq.trade.bisq_easy.protocol.messages.*;
import bisq.trade.protocol.Protocol;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@Getter
public class BisqEasyTradeService implements PersistenceClient<BisqEasyTradeStore>, Service, MessageListener {
    @Getter
    private final BisqEasyTradeStore persistableStore = new BisqEasyTradeStore();
    @Getter
    private final Persistence<BisqEasyTradeStore> persistence;
    private final ServiceProvider serviceProvider;

    // We don't persist the protocol, only the model.
    private final Map<String, BisqEasyProtocol> tradeProtocolById = new ConcurrentHashMap<>();

    public BisqEasyTradeService(ServiceProvider serviceProvider) {
        persistence = serviceProvider.getPersistenceService().getOrCreatePersistence(this, persistableStore);
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
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof BisqEasyTakeOfferRequest) {
            onBisqEasyTakeOfferMessage((BisqEasyTakeOfferRequest) networkMessage);
        } else if (networkMessage instanceof BisqEasyTakeOfferResponse) {
            onBisqEasyTakeOfferResponse((BisqEasyTakeOfferResponse) networkMessage);
        } else if (networkMessage instanceof BisqEasyAccountDataMessage) {
            onBisqEasySendAccountDataMessage((BisqEasyAccountDataMessage) networkMessage);
        } else if (networkMessage instanceof BisqEasyConfirmFiatSentMessage) {
            onBisqEasyConfirmFiatSentMessage((BisqEasyConfirmFiatSentMessage) networkMessage);
        } else if (networkMessage instanceof BisqEasyBtcAddressMessage) {
            onBisqEasyBtcAddressMessage((BisqEasyBtcAddressMessage) networkMessage);
        } else if (networkMessage instanceof BisqEasyConfirmFiatReceiptMessage) {
            onBisqEasyConfirmFiatReceiptMessage((BisqEasyConfirmFiatReceiptMessage) networkMessage);
        } else if (networkMessage instanceof BisqEasyConfirmBtcSentMessage) {
            onBisqEasyConfirmBtcSentMessage((BisqEasyConfirmBtcSentMessage) networkMessage);
        }

    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Message event
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void onBisqEasyTakeOfferMessage(BisqEasyTakeOfferRequest message) {
        NetworkId sender = checkNotNull(message.getSender());
        BisqEasyContract bisqEasyContract = checkNotNull(message.getBisqEasyContract());
        boolean isBuyer = bisqEasyContract.getOffer().getMakersDirection().isBuy();
        Identity myIdentity = serviceProvider.getIdentityService().findAnyIdentityByNodeId(bisqEasyContract.getOffer().getMakerNetworkId().getNodeId()).orElseThrow();
        BisqEasyTrade tradeModel = new BisqEasyTrade(isBuyer, false, myIdentity, bisqEasyContract, sender);

        if (findProtocol(tradeModel.getId()).isPresent()) {
            log.error("We received the BisqEasyTakeOfferRequest for an already existing protocol");
            return;
        }
        persistableStore.add(tradeModel);

        Protocol<BisqEasyTrade> protocol = createAndAddTradeProtocol(tradeModel);
        try {
            protocol.handle(message);
            persist();
        } catch (TradeException e) {
            log.error("Error at processing message " + message, e);
        }
    }

    private void onBisqEasyTakeOfferResponse(BisqEasyTakeOfferResponse message) {
        findProtocol(message.getTradeId()).ifPresent(protocol -> {
            try {
                protocol.handle(message);
                persist();
            } catch (TradeException e) {
                log.error("Error at processing message " + message, e);
            }
        });
    }

    private void onBisqEasySendAccountDataMessage(BisqEasyAccountDataMessage message) {
        findProtocol(message.getTradeId()).ifPresent(protocol -> {
            try {
                protocol.handle(message);
                persist();
            } catch (TradeException e) {
                log.error("Error at processing message " + message, e);
            }
        });
    }

    private void onBisqEasyConfirmFiatSentMessage(BisqEasyConfirmFiatSentMessage message) {
        findProtocol(message.getTradeId()).ifPresent(protocol -> {
            try {
                protocol.handle(message);
                persist();
            } catch (TradeException e) {
                log.error("Error at processing message " + message, e);
            }
        });
    }

    private void onBisqEasyBtcAddressMessage(BisqEasyBtcAddressMessage message) {
        findProtocol(message.getTradeId()).ifPresent(protocol -> {
            try {
                protocol.handle(message);
                persist();
            } catch (TradeException e) {
                log.error("Error at processing message " + message, e);
            }
        });
    }

    private void onBisqEasyConfirmFiatReceiptMessage(BisqEasyConfirmFiatReceiptMessage message) {
        findProtocol(message.getTradeId()).ifPresent(protocol -> {
            try {
                protocol.handle(message);
                persist();
            } catch (TradeException e) {
                log.error("Error at processing message " + message, e);
            }
        });
    }

    private void onBisqEasyConfirmBtcSentMessage(BisqEasyConfirmBtcSentMessage message) {
        findProtocol(message.getTradeId()).ifPresent(protocol -> {
            try {
                protocol.handle(message);
                persist();
            } catch (TradeException e) {
                log.error("Error at processing message " + message, e);
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public BisqEasyTrade onTakeOffer(Identity takerIdentity,
                                     BisqEasyOffer bisqEasyOffer,
                                     Monetary baseSideAmount,
                                     Monetary quoteSideAmount,
                                     BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec,
                                     FiatPaymentMethodSpec fiatPaymentMethodSpec) throws TradeException {
        Optional<UserProfile> mediator = serviceProvider.getSupportService().getMediationService().selectMediator(bisqEasyOffer.getMakersUserProfileId(), takerIdentity.getId());
        NetworkId takerNetworkId = takerIdentity.getNetworkId();
        BisqEasyContract contract = new BisqEasyContract(bisqEasyOffer,
                takerNetworkId,
                baseSideAmount.getValue(),
                quoteSideAmount.getValue(),
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator);
        boolean isBuyer = bisqEasyOffer.getTakersDirection().isBuy();
        BisqEasyTrade tradeModel = new BisqEasyTrade(isBuyer, true, takerIdentity, contract, takerNetworkId);

        checkArgument(findProtocol(tradeModel.getId()).isEmpty(),
                "We received the BisqEasyTakeOfferRequest for an already existing protocol");

        persistableStore.add(tradeModel);

        Protocol<BisqEasyTrade> protocol = createAndAddTradeProtocol(tradeModel);
        protocol.handle(new BisqEasyTakeOfferEvent(contract));
        persist();
        return tradeModel;
    }

    public void sellerSendsPaymentAccount(BisqEasyTrade tradeModel, String paymentAccountData) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasyAccountDataEvent(paymentAccountData));
        persist();
    }

    public void buyerConfirmFiatSent(BisqEasyTrade tradeModel) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasyConfirmFiatSentEvent());
        persist();
    }

    public void buyerSendBtcAddress(BisqEasyTrade tradeModel, String buyersBtcAddress) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasySendBtcAddressEvent(buyersBtcAddress));
        persist();
    }

    public void sellerConfirmFiatReceipt(BisqEasyTrade tradeModel) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasyConfirmFiatReceiptEvent());
        persist();
    }

    public void sellerConfirmBtcSent(BisqEasyTrade tradeModel, String txId) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasyConfirmBtcSentEvent(txId));
        persist();
    }

    public void btcConfirmed(BisqEasyTrade tradeModel) throws TradeException {
        BisqEasyProtocol protocol = findProtocol(tradeModel.getId()).orElseThrow();
        protocol.handle(new BisqEasyBtcConfirmedEvent());
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Misc API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<BisqEasyProtocol> findProtocol(String id) {
        return Optional.ofNullable(tradeProtocolById.get(id));
    }

    public Optional<BisqEasyTrade> findTrade(String tradeId) {
        return persistableStore.findTrade(tradeId);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // TradeProtocol factory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private BisqEasyProtocol createAndAddTradeProtocol(BisqEasyTrade model) {
        String id = model.getId();
        BisqEasyProtocol tradeProtocol;
        boolean isBuyer = model.isBuyer();
        if (model.isTaker()) {
            if (isBuyer) {
                tradeProtocol = new BisqEasyBuyerAsTakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new BisqEasySellerAsTakerProtocol(serviceProvider, model);
            }
        } else {
            if (isBuyer) {
                tradeProtocol = new BisqEasyBuyerAsMakerProtocol(serviceProvider, model);
            } else {
                tradeProtocol = new BisqEasySellerAsMakerProtocol(serviceProvider, model);
            }
        }
        tradeProtocolById.put(id, tradeProtocol);
        return tradeProtocol;
    }
}