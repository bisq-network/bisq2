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
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.identity.NetworkId;
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
import bisq.user.banned.BannedUserService;
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
    private final BannedUserService bannedUserService;

    public BisqEasyTradeService(ServiceProvider serviceProvider) {
        persistence = serviceProvider.getPersistenceService().getOrCreatePersistence(this, persistableStore);
        this.serviceProvider = serviceProvider;
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        serviceProvider.getNetworkService().addMessageListener(this);

        persistableStore.getTrades().forEach(this::createAndAddTradeProtocol);

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
        if (envelopePayloadMessage instanceof BisqEasyTradeMessage) {
            BisqEasyTradeMessage bisqEasyTradeMessage = (BisqEasyTradeMessage) envelopePayloadMessage;
            if (bannedUserService.isNetworkIdBanned(bisqEasyTradeMessage.getSender())) {
                log.warn("Message ignored as sender is banned");
                return;
            }

            if (bisqEasyTradeMessage instanceof BisqEasyTakeOfferRequest) {
                onBisqEasyTakeOfferMessage((BisqEasyTakeOfferRequest) bisqEasyTradeMessage);
            } else if (bisqEasyTradeMessage instanceof BisqEasyTakeOfferResponse) {
                onBisqEasyTakeOfferResponse((BisqEasyTakeOfferResponse) bisqEasyTradeMessage);
            } else if (bisqEasyTradeMessage instanceof BisqEasyAccountDataMessage) {
                onBisqEasySendAccountDataMessage((BisqEasyAccountDataMessage) bisqEasyTradeMessage);
            } else if (bisqEasyTradeMessage instanceof BisqEasyConfirmFiatSentMessage) {
                onBisqEasyConfirmFiatSentMessage((BisqEasyConfirmFiatSentMessage) bisqEasyTradeMessage);
            } else if (bisqEasyTradeMessage instanceof BisqEasyBtcAddressMessage) {
                onBisqEasyBtcAddressMessage((BisqEasyBtcAddressMessage) bisqEasyTradeMessage);
            } else if (bisqEasyTradeMessage instanceof BisqEasyConfirmFiatReceiptMessage) {
                onBisqEasyConfirmFiatReceiptMessage((BisqEasyConfirmFiatReceiptMessage) bisqEasyTradeMessage);
            } else if (bisqEasyTradeMessage instanceof BisqEasyConfirmBtcSentMessage) {
                onBisqEasyConfirmBtcSentMessage((BisqEasyConfirmBtcSentMessage) bisqEasyTradeMessage);
            }
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
        BisqEasyTrade bisqEasyTrade = new BisqEasyTrade(isBuyer, false, myIdentity, bisqEasyContract, sender);

        if (findProtocol(bisqEasyTrade.getId()).isPresent()) {
            log.error("We received the BisqEasyTakeOfferRequest for an already existing protocol");
            return;
        }
        checkArgument(!hadTrade(bisqEasyTrade.getId()), "Trade has been already taken");
        persistableStore.add(bisqEasyTrade);

        try {
            createAndAddTradeProtocol(bisqEasyTrade).handle(message);
            persist();
        } catch (Exception e) {
            log.error("Error at processing message " + message, e);
        }
    }

    private void onBisqEasyTakeOfferResponse(BisqEasyTakeOfferResponse message) {
        try {
            getProtocol(message.getTradeId()).handle(message);
            persist();
        } catch (Exception e) {
            log.error("Error at processing message " + message, e);
        }
    }

    private void onBisqEasySendAccountDataMessage(BisqEasyAccountDataMessage message) {
        try {
            getProtocol(message.getTradeId()).handle(message);
            persist();
        } catch (Exception e) {
            log.error("Error at processing message " + message, e);
        }
    }

    private void onBisqEasyConfirmFiatSentMessage(BisqEasyConfirmFiatSentMessage message) {
        try {
            getProtocol(message.getTradeId()).handle(message);
            persist();
        } catch (Exception e) {
            log.error("Error at processing message " + message, e);
        }
    }

    private void onBisqEasyBtcAddressMessage(BisqEasyBtcAddressMessage message) {
        try {
            getProtocol(message.getTradeId()).handle(message);
            persist();
        } catch (Exception e) {
            log.error("Error at processing message " + message, e);
        }
    }

    private void onBisqEasyConfirmFiatReceiptMessage(BisqEasyConfirmFiatReceiptMessage message) {
        try {
            getProtocol(message.getTradeId()).handle(message);
            persist();
        } catch (Exception e) {
            log.error("Error at processing message " + message, e);
        }
    }

    private void onBisqEasyConfirmBtcSentMessage(BisqEasyConfirmBtcSentMessage message) {
        try {
            getProtocol(message.getTradeId()).handle(message);
            persist();
        } catch (Exception e) {
            log.error("Error at processing message " + message, e);
        }
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
        BisqEasyTrade bisqEasyTrade = new BisqEasyTrade(isBuyer, true, takerIdentity, contract, takerNetworkId);

        checkArgument(findProtocol(bisqEasyTrade.getId()).isEmpty(),
                "We received the BisqEasyTakeOfferRequest for an already existing protocol");

        checkArgument(!hadTrade(bisqEasyTrade.getId()), "Trade has been already taken");
        persistableStore.add(bisqEasyTrade);

        createAndAddTradeProtocol(bisqEasyTrade).handle(new BisqEasyTakeOfferEvent(contract));
        persist();
        return bisqEasyTrade;
    }

    public void sellerSendsPaymentAccount(BisqEasyTrade trade, String paymentAccountData) throws TradeException {
        getProtocol(trade.getId()).handle(new BisqEasyAccountDataEvent(paymentAccountData));
        persist();
    }

    public void buyerConfirmFiatSent(BisqEasyTrade trade) throws TradeException {
        getProtocol(trade.getId()).handle(new BisqEasyConfirmFiatSentEvent());
        persist();
    }

    public void buyerSendBtcAddress(BisqEasyTrade trade, String buyersBtcAddress) throws TradeException {
        getProtocol(trade.getId()).handle(new BisqEasySendBtcAddressEvent(buyersBtcAddress));
        persist();
    }

    public void sellerConfirmFiatReceipt(BisqEasyTrade trade) throws TradeException {
        getProtocol(trade.getId()).handle(new BisqEasyConfirmFiatReceiptEvent());
        persist();
    }

    public void sellerConfirmBtcSent(BisqEasyTrade trade, String txId) throws TradeException {
        getProtocol(trade.getId()).handle(new BisqEasyConfirmBtcSentEvent(txId));
        persist();
    }

    public void btcConfirmed(BisqEasyTrade trade) throws TradeException {
        getProtocol(trade.getId()).handle(new BisqEasyBtcConfirmedEvent());
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Misc API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<BisqEasyProtocol> findProtocol(String id) {
        return Optional.ofNullable(tradeProtocolById.get(id));
    }

    public BisqEasyProtocol getProtocol(String id) throws IllegalArgumentException {
        Optional<BisqEasyProtocol> protocol = findProtocol(id);
        checkArgument(protocol.isPresent(), "No protocol found for trade ID " + id);
        return protocol.get();
    }

    public Optional<BisqEasyTrade> findTrade(String tradeId) {
        return persistableStore.findTrade(tradeId);
    }

    public boolean hadTrade(String tradeId) {
        return persistableStore.hadTrade(tradeId);
    }

    public ObservableSet<BisqEasyTrade> getTrades() {
        return persistableStore.getTrades();
    }

    public void removeTrade(BisqEasyTrade trade) {
        persistableStore.removeTrade(trade);
        tradeProtocolById.remove(trade.getId());
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // TradeProtocol factory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private BisqEasyProtocol createAndAddTradeProtocol(BisqEasyTrade trade) {
        String id = trade.getId();
        BisqEasyProtocol tradeProtocol;
        boolean isBuyer = trade.isBuyer();
        if (trade.isTaker()) {
            if (isBuyer) {
                tradeProtocol = new BisqEasyBuyerAsTakerProtocol(serviceProvider, trade);
            } else {
                tradeProtocol = new BisqEasySellerAsTakerProtocol(serviceProvider, trade);
            }
        } else {
            if (isBuyer) {
                tradeProtocol = new BisqEasyBuyerAsMakerProtocol(serviceProvider, trade);
            } else {
                tradeProtocol = new BisqEasySellerAsMakerProtocol(serviceProvider, trade);
            }
        }
        tradeProtocolById.put(id, tradeProtocol);
        return tradeProtocol;
    }
}