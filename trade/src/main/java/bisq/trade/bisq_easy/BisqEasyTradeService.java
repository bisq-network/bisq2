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

import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.fsm.Event;
import bisq.common.monetary.Monetary;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.platform.Version;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.*;
import bisq.trade.bisq_easy.protocol.events.*;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyAccountDataMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyBtcAddressMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.*;

@Slf4j
@Getter
public class BisqEasyTradeService implements PersistenceClient<BisqEasyTradeStore>, Service, ConfidentialMessageService.Listener {
    public final static double MAX_TRADE_PRICE_DEVIATION = 0.1; // 10%
    @Getter
    private final BisqEasyTradeStore persistableStore = new BisqEasyTradeStore();
    @Getter
    private final Persistence<BisqEasyTradeStore> persistence;
    private final ServiceProvider serviceProvider;
    private final BannedUserService bannedUserService;

    // We don't persist the protocol, only the model.
    private final Map<String, BisqEasyProtocol> tradeProtocolById = new ConcurrentHashMap<>();
    private final AlertService alertService;
    private boolean haltTrading;
    private boolean requireVersionForTrading;
    private Optional<String> minVersion = Optional.empty();

    public BisqEasyTradeService(ServiceProvider serviceProvider) {
        persistence = serviceProvider.getPersistenceService().getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.serviceProvider = serviceProvider;
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        alertService = serviceProvider.getBondedRolesService().getAlertService();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        serviceProvider.getNetworkService().addConfidentialMessageListener(this);

        persistableStore.getTrades().forEach(this::createAndAddTradeProtocol);
        alertService.getAuthorizedAlertDataSet().addObserver(new CollectionObserver<>() {
            @Override
            public void add(AuthorizedAlertData authorizedAlertData) {
                if (authorizedAlertData.getAlertType() == AlertType.EMERGENCY) {
                    if (authorizedAlertData.isHaltTrading()) {
                        haltTrading = true;
                    }
                    if (authorizedAlertData.isRequireVersionForTrading()) {
                        requireVersionForTrading = true;
                        minVersion = authorizedAlertData.getMinVersion();
                    }
                }
            }

            @Override
            public void remove(Object element) {
                if (element instanceof AuthorizedAlertData authorizedAlertData) {
                    if (authorizedAlertData.getAlertType() == AlertType.EMERGENCY) {
                        if (authorizedAlertData.isHaltTrading()) {
                            haltTrading = false;
                        }
                        if (authorizedAlertData.isRequireVersionForTrading()) {
                            requireVersionForTrading = false;
                            minVersion = Optional.empty();
                        }
                    }
                }
            }

            @Override
            public void clear() {
                haltTrading = false;
                requireVersionForTrading = false;
                minVersion = Optional.empty();
            }
        });

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
        if (envelopePayloadMessage instanceof BisqEasyTradeMessage bisqEasyTradeMessage) {
            verifyTradingNotOnHalt();
            verifyMinVersionForTrading();

            if (bannedUserService.isNetworkIdBanned(bisqEasyTradeMessage.getSender())) {
                log.warn("Message ignored as sender is banned");
                return;
            }

            if (bisqEasyTradeMessage instanceof BisqEasyTakeOfferRequest) {
                onBisqEasyTakeOfferMessage((BisqEasyTakeOfferRequest) bisqEasyTradeMessage);
            } else if (bisqEasyTradeMessage instanceof BisqEasyBtcAddressMessage) {
                onBisqEasyBtcAddressMessage((BisqEasyBtcAddressMessage) bisqEasyTradeMessage);
            } else if (bisqEasyTradeMessage instanceof BisqEasyAccountDataMessage) {
                onBisqEasySendAccountDataMessage((BisqEasyAccountDataMessage) bisqEasyTradeMessage);
            } else {
                handleBisqEasyTradeMessage(bisqEasyTradeMessage);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Message event
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void onBisqEasyTakeOfferMessage(BisqEasyTakeOfferRequest message) {
        BisqEasyContract bisqEasyContract = checkNotNull(message.getBisqEasyContract());
        BisqEasyProtocol protocol = getOrCreateProtocol(message.getTradeId(),
                bisqEasyContract.getOffer(),
                message.getSender(),
                message.getReceiver());
        checkArgument(protocol.getModel().getContract() == null,
                "Trade must not have a contract set yet.");
        protocol.getModel().setContract(bisqEasyContract);
        handleEvent(protocol, message);
    }

    private void onBisqEasyBtcAddressMessage(BisqEasyBtcAddressMessage message) {
        BisqEasyProtocol protocol = getOrCreateProtocol(message.getTradeId(),
                message.getBisqEasyOffer(),
                message.getSender(),
                message.getReceiver());
        handleEvent(protocol, message);
    }

    private void onBisqEasySendAccountDataMessage(BisqEasyAccountDataMessage message) {
        BisqEasyProtocol protocol = getOrCreateProtocol(message.getTradeId(),
                message.getBisqEasyOffer(),
                message.getSender(),
                message.getReceiver());
        handleEvent(protocol, message);
    }

    private BisqEasyProtocol getOrCreateProtocol(String tradeId, BisqEasyOffer offer, NetworkId sender, NetworkId receiver) {
        return findProtocol(tradeId).isPresent()
                ? getProtocol(tradeId)
                : createTradeProtocol(offer, sender, receiver);
    }

    private BisqEasyProtocol createTradeProtocol(BisqEasyOffer bisqEasyOffer, NetworkId sender, NetworkId receiver) {
        // We only create the data required for the protocol creation.
        // Verification will happen in the BisqEasyTakeOfferRequestHandler
        boolean isBuyer = bisqEasyOffer.getMakersDirection().isBuy();
        Identity myIdentity = serviceProvider.getIdentityService().findAnyIdentityByNetworkId(bisqEasyOffer.getMakerNetworkId()).orElseThrow();
        BisqEasyTrade bisqEasyTrade = new BisqEasyTrade(isBuyer, false, myIdentity, bisqEasyOffer, sender, receiver);
        String tradeId = bisqEasyTrade.getId();
        checkArgument(findProtocol(tradeId).isEmpty(), "We received the BisqEasyTakeOfferRequest for an already existing protocol");
        checkArgument(!tradeExists(tradeId), "A trade with that ID exists already");
        persistableStore.addTrade(bisqEasyTrade);
        return createAndAddTradeProtocol(bisqEasyTrade);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public BisqEasyProtocol createBisqEasyProtocol(Identity takerIdentity,
                                                   BisqEasyOffer bisqEasyOffer,
                                                   Monetary baseSideAmount,
                                                   Monetary quoteSideAmount,
                                                   BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec,
                                                   FiatPaymentMethodSpec fiatPaymentMethodSpec,
                                                   Optional<UserProfile> mediator,
                                                   PriceSpec agreedPriceSpec,
                                                   long marketPrice) {
        verifyTradingNotOnHalt();
        verifyMinVersionForTrading();

        NetworkId takerNetworkId = takerIdentity.getNetworkId();
        BisqEasyContract contract = new BisqEasyContract(
                System.currentTimeMillis(),
                bisqEasyOffer,
                takerNetworkId,
                baseSideAmount.getValue(),
                quoteSideAmount.getValue(),
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator,
                agreedPriceSpec,
                marketPrice);
        boolean isBuyer = bisqEasyOffer.getTakersDirection().isBuy();
        NetworkId makerNetworkId = contract.getMaker().getNetworkId();
        BisqEasyTrade bisqEasyTrade = new BisqEasyTrade(isBuyer, true, takerIdentity, bisqEasyOffer, takerNetworkId, makerNetworkId);
        bisqEasyTrade.setContract(contract);

        checkArgument(findProtocol(bisqEasyTrade.getId()).isEmpty(),
                "We received the BisqEasyTakeOfferRequest for an already existing protocol");

        checkArgument(!tradeExists(bisqEasyTrade.getId()), "A trade with that ID exists already");
        persistableStore.addTrade(bisqEasyTrade);

        return createAndAddTradeProtocol(bisqEasyTrade);
    }

    public void takeOffer(BisqEasyTrade trade) {
        handleBisqEasyTradeEvent(trade, new BisqEasyTakeOfferEvent());
    }

    public void sellerSendsPaymentAccount(BisqEasyTrade trade, String paymentAccountData) {
        handleBisqEasyTradeEvent(trade, new BisqEasyAccountDataEvent(paymentAccountData));
    }

    public void buyerConfirmFiatSent(BisqEasyTrade trade) {
        handleBisqEasyTradeEvent(trade, new BisqEasyConfirmFiatSentEvent());
    }

    public void buyerSendBitcoinPaymentData(BisqEasyTrade trade, String buyersBitcoinPaymentData) {
        handleBisqEasyTradeEvent(trade, new BisqEasySendBtcAddressEvent(buyersBitcoinPaymentData));
    }

    public void sellerConfirmFiatReceipt(BisqEasyTrade trade) {
        handleBisqEasyTradeEvent(trade, new BisqEasyConfirmFiatReceiptEvent());
    }

    public void sellerConfirmBtcSent(BisqEasyTrade trade, Optional<String> paymentProof) {
        handleBisqEasyTradeEvent(trade, new BisqEasyConfirmBtcSentEvent(paymentProof));
    }

    public void btcConfirmed(BisqEasyTrade trade) {
        handleBisqEasyTradeEvent(trade, new BisqEasyBtcConfirmedEvent());
    }

    public void rejectTrade(BisqEasyTrade trade) {
        handleBisqEasyTradeEvent(trade, new BisqEasyRejectTradeEvent());
    }

    public void cancelTrade(BisqEasyTrade trade) {
        handleBisqEasyTradeEvent(trade, new BisqEasyCancelTradeEvent());
    }

    private void handleBisqEasyTradeEvent(BisqEasyTrade trade, BisqEasyTradeEvent event) {
        verifyTradingNotOnHalt();
        verifyMinVersionForTrading();
        handleEvent(getProtocol(trade.getId()), event);
    }

    private void handleBisqEasyTradeMessage(BisqEasyTradeMessage message) {
        String tradeId = message.getTradeId();
        findProtocol(tradeId).ifPresentOrElse(protocol -> handleEvent(protocol, message),
                () -> log.info("Protocol with tradeId {} not found. This is expected if the trade have been closed already", tradeId));
    }

    private void handleEvent(BisqEasyProtocol protocol, Event event) {
        protocol.handle(event);
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

    public boolean tradeExists(String tradeId) {
        return persistableStore.tradeExists(tradeId);
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
        trade.setProtocolVersion(tradeProtocol.getVersion());
        tradeProtocolById.put(id, tradeProtocol);
        return tradeProtocol;
    }

    private void verifyTradingNotOnHalt() {
        checkArgument(!haltTrading, "Trading is on halt for security reasons. " +
                "The Bisq security manager has published an emergency alert with haltTrading set to true");
    }

    private void verifyMinVersionForTrading() {
        if (requireVersionForTrading && minVersion.isPresent()) {
            checkArgument(ApplicationVersion.getVersion().aboveOrEqual(new Version(minVersion.get())),
                    "For trading you need to have version " + minVersion.get() + " installed. " +
                            "The Bisq security manager has published an emergency alert with a min. version required for trading.");
        }
    }
}
