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

package bisq.trade.mu_sig;

import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.monetary.Monetary;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.platform.Version;
import bisq.common.timer.Scheduler;
import bisq.contract.bisq_musig.MuSigContract;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.offer.musig.MuSigOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.settings.SettingsService;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import bisq.trade.mu_sig.events.MuSigTradeEvent;
import bisq.trade.mu_sig.events.buyer_as_taker.MuSigPaymentInitiatedEvent;
import bisq.trade.mu_sig.events.buyer_as_taker.MuSigTakeOfferEvent;
import bisq.trade.mu_sig.messages.network.MuSigTradeMessage;
import bisq.trade.mu_sig.messages.network.not_used_yet.MuSigTakeOfferRequest;
import bisq.trade.mu_sig.protocol.MuSigBuyerAsTakerProtocol;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.trade.mu_sig.protocol.MuSigSellerAsMakerProtocol;
import bisq.trade.mu_sig.protocol.ignore.MuSigBuyerAsMakerProtocol;
import bisq.trade.mu_sig.protocol.ignore.MuSigSellerAsTakerProtocol;
import bisq.trade.protobuf.MusigGrpc;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public class MuSigTradeService implements PersistenceClient<MuSigTradeStore>, Service, ConfidentialMessageService.Listener {
    private final ServiceProvider serviceProvider;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final SettingsService settingsService;
    private final BannedUserService bannedUserService;
    private final AlertService alertService;

    @Getter
    private final MuSigTradeStore persistableStore = new MuSigTradeStore();
    @Getter
    private final Persistence<MuSigTradeStore> persistence;

    // We don't persist the protocol, only the model.
    private final Map<String, MuSigProtocol> tradeProtocolById = new ConcurrentHashMap<>();

    private boolean haltTrading;
    private boolean requireVersionForTrading;
    private Optional<String> minRequiredVersionForTrading = Optional.empty();

    private Pin authorizedAlertDataSetPin, numDaysAfterRedactingTradeDataPin;
    private Scheduler numDaysAfterRedactingTradeDataScheduler;
    private final Set<MuSigTradeMessage> pendingMessages = new CopyOnWriteArraySet<>();
    private final Map<String, Scheduler> cooperativeCloseTimeoutSchedulerByTradeId = new ConcurrentHashMap<>();
    @Getter
    private MusigGrpc.MusigBlockingStub musigStub;
    private ManagedChannel grpcChannel;

    public MuSigTradeService(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        settingsService = serviceProvider.getSettingsService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        alertService = serviceProvider.getBondedRolesService().getAlertService();

        persistence = serviceProvider.getPersistenceService().getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        persistableStore.getTrades().forEach(this::createAndAddTradeProtocol);

        networkService.getConfidentialMessageServices().stream()
                .flatMap(service -> service.getProcessedEnvelopePayloadMessages().stream())
                .forEach(this::onMessage);
        networkService.addConfidentialMessageListener(this);

        //todo add host/port to config
        grpcChannel = Grpc.newChannelBuilderForAddress(
                "127.0.0.1",
                50051,
                InsecureChannelCredentials.create()
        ).build();

        try {
            musigStub = MusigGrpc.newBlockingStub(grpcChannel);
        } finally {
            grpcChannel.shutdown();
        }

        authorizedAlertDataSetPin = alertService.getAuthorizedAlertDataSet().addObserver(new CollectionObserver<>() {
            @Override
            public void add(AuthorizedAlertData authorizedAlertData) {
                if (authorizedAlertData.getAlertType() == AlertType.EMERGENCY) {
                    if (authorizedAlertData.isHaltTrading()) {
                        haltTrading = true;
                    }
                    if (authorizedAlertData.isRequireVersionForTrading()) {
                        requireVersionForTrading = true;
                        minRequiredVersionForTrading = authorizedAlertData.getMinVersion();
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
                            minRequiredVersionForTrading = Optional.empty();
                        }
                    }
                }
            }

            @Override
            public void clear() {
                haltTrading = false;
                requireVersionForTrading = false;
                minRequiredVersionForTrading = Optional.empty();
            }
        });

        numDaysAfterRedactingTradeDataScheduler = Scheduler.run(this::maybeRedactDataOfCompletedTrades).periodically(1, TimeUnit.HOURS);
        numDaysAfterRedactingTradeDataPin = settingsService.getNumDaysAfterRedactingTradeData().addObserver(numDays -> maybeRedactDataOfCompletedTrades());

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (authorizedAlertDataSetPin != null) {
            authorizedAlertDataSetPin.unbind();
            authorizedAlertDataSetPin = null;
        }
        if (numDaysAfterRedactingTradeDataPin != null) {
            numDaysAfterRedactingTradeDataPin.unbind();
            numDaysAfterRedactingTradeDataPin = null;
        }
        if (numDaysAfterRedactingTradeDataScheduler != null) {
            numDaysAfterRedactingTradeDataScheduler.stop();
            numDaysAfterRedactingTradeDataScheduler = null;
        }
        if (grpcChannel != null) {
            grpcChannel.shutdown();
            grpcChannel = null;
        }

        networkService.removeConfidentialMessageListener(this);

        cooperativeCloseTimeoutSchedulerByTradeId.values().forEach(Scheduler::stop);
        cooperativeCloseTimeoutSchedulerByTradeId.clear();

        return CompletableFuture.completedFuture(true);
    }
    /* --------------------------------------------------------------------- */
    // MessageListener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MuSigTradeMessage muSigTradeMessage) {
            verifyTradingNotOnHalt();
            verifyMinVersionForTrading();

            if (bannedUserService.isNetworkIdBanned(muSigTradeMessage.getSender())) {
                log.warn("Message ignored as sender is banned");
                return;
            }

            if (muSigTradeMessage instanceof MuSigTakeOfferRequest) {
                handleMuSigTakeOfferMessage((MuSigTakeOfferRequest) muSigTradeMessage);
            } else {
                handleMuSigTradeMessage(muSigTradeMessage);
            }
        }
    }


    /* --------------------------------------------------------------------- */
    // Message event
    /* --------------------------------------------------------------------- */

    private void handleMuSigTakeOfferMessage(MuSigTakeOfferRequest message) {
        MuSigContract MuSigContract = message.getMuSigContract();
        MuSigProtocol protocol = createProtocol(MuSigContract, message.getSender(), message.getReceiver());
        protocol.handle(message);
        persist();

        if (!pendingMessages.isEmpty()) {
            log.info("We have pendingMessages. We try to re-process them now.");
            pendingMessages.forEach(this::handleMuSigTradeMessage);
        }
    }

    private void handleMuSigTradeMessage(MuSigTradeMessage message) {
        String tradeId = message.getTradeId();
        findProtocol(tradeId).ifPresentOrElse(protocol -> {
                    protocol.handle(message);
                    persist();

                    if (pendingMessages.contains(message)) {
                        log.info("We remove message {} from pendingMessages.", message);
                        pendingMessages.remove(message);
                    }

                    if (!pendingMessages.isEmpty()) {
                        log.info("We have pendingMessages. We try to re-process them now.");
                        pendingMessages.forEach(this::handleMuSigTradeMessage);
                    }
                },
                () -> {
                    log.info("Protocol with tradeId {} not found. We add the message to pendingMessages for " +
                            "re-processing when the next message arrives. message={}", tradeId, message);
                    pendingMessages.add(message);
                });
    }


    /* --------------------------------------------------------------------- */
    // Events
    /* --------------------------------------------------------------------- */

    public MuSigProtocol createMuSigProtocol(Identity takerIdentity,
                                             MuSigOffer muSigOffer,
                                             Monetary baseSideAmount,
                                             Monetary quoteSideAmount,
                                             BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec,
                                             FiatPaymentMethodSpec fiatPaymentMethodSpec,
                                             Optional<UserProfile> mediator,
                                             PriceSpec priceSpec,
                                             long marketPrice) {
        verifyTradingNotOnHalt();
        verifyMinVersionForTrading();

        NetworkId takerNetworkId = takerIdentity.getNetworkId();
        MuSigContract contract = new MuSigContract(
                System.currentTimeMillis(),
                muSigOffer,
                takerNetworkId,
                baseSideAmount.getValue(),
                quoteSideAmount.getValue(),
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator,
                priceSpec,
                marketPrice);
        boolean isBuyer = muSigOffer.getTakersDirection().isBuy();
        NetworkId makerNetworkId = contract.getMaker().getNetworkId();
        MuSigTrade muSigTrade = new MuSigTrade(contract, isBuyer, true, takerIdentity, muSigOffer, takerNetworkId, makerNetworkId);
        checkArgument(findProtocol(muSigTrade.getId()).isEmpty(),
                "We received the MuSigTakeOfferRequest for an already existing protocol");

        checkArgument(!tradeExists(muSigTrade.getId()), "A trade with that ID exists already");
        persistableStore.addTrade(muSigTrade);

        return createAndAddTradeProtocol(muSigTrade);
    }

    public void takeOffer(MuSigTrade trade) {
        handleMuSigTradeEvent(trade, new MuSigTakeOfferEvent());
    }

    public void buyerConfirmFiatSent(MuSigTrade trade) {
        handleMuSigTradeEvent(trade, new MuSigPaymentInitiatedEvent());
    }


    public void startCooperativeCloseTimeout(MuSigTrade trade, MuSigTradeEvent event) {
        stopCooperativeCloseTimeout(trade);
        cooperativeCloseTimeoutSchedulerByTradeId.put(trade.getId(),
                Scheduler.run(() ->
                                handleMuSigTradeEvent(trade, event))
                        .after(1000));
    }

    public void stopCooperativeCloseTimeout(MuSigTrade trade) {
        String tradeId = trade.getId();
        if (cooperativeCloseTimeoutSchedulerByTradeId.containsKey(tradeId)) {
            cooperativeCloseTimeoutSchedulerByTradeId.get(tradeId).stop();
        }
    }

    private void handleMuSigTradeEvent(MuSigTrade trade, MuSigTradeEvent event) {
        verifyTradingNotOnHalt();
        verifyMinVersionForTrading();
        String tradeId = trade.getId();
        findProtocol(tradeId).ifPresentOrElse(protocol -> {
                    protocol.handle(event);
                    persist();
                },
                () -> log.info("Protocol with tradeId {} not found. This is expected if the trade have been closed already", tradeId));
    }


    /* --------------------------------------------------------------------- */
    // Misc API
    /* --------------------------------------------------------------------- */

    public Optional<MuSigProtocol> findProtocol(String id) {
        return Optional.ofNullable(tradeProtocolById.get(id));
    }

    public Optional<MuSigTrade> findTrade(String tradeId) {
        return persistableStore.findTrade(tradeId);
    }

    public boolean tradeExists(String tradeId) {
        return persistableStore.tradeExists(tradeId);
    }

    public boolean wasOfferAlreadyTaken(MuSigOffer muSigOffer, NetworkId takerNetworkId) {
        if (new Date().after(Trade.TRADE_ID_V1_ACTIVATION_DATE)) {
            // We do only check if we have a trade with same offer and takerNetworkId.
            // As we include after TRADE_ID_V1_ACTIVATION_DATE the takeOffer date in the trade ID we might have
            // multiple trades with the same offer and takerNetworkId combination.
            // To verify that we do not have the same trade we need to use the tradeExists method.
            return getTrades().stream().anyMatch(trade ->
                    trade.getOffer().getId().equals(muSigOffer.getId()) &&
                            trade.getTaker().getNetworkId().getId().equals(takerNetworkId.getId())
            );
        } else {
            String tradeId = Trade.createId(muSigOffer.getId(), takerNetworkId.getId());
            return tradeExists(tradeId);
        }
    }

    public Collection<MuSigTrade> getTrades() {
        return persistableStore.getTrades();
    }

    public void removeTrade(MuSigTrade trade) {
        persistableStore.removeTrade(trade.getId());
        tradeProtocolById.remove(trade.getId());
        persist();
    }


    /* --------------------------------------------------------------------- */
    // TradeProtocol factory
    /* --------------------------------------------------------------------- */

    private MuSigProtocol createProtocol(MuSigContract contract, NetworkId sender, NetworkId receiver) {
        // We only create the data required for the protocol creation.
        // Verification will happen in the MuSigTakeOfferRequestHandler
        MuSigOffer offer = contract.getOffer();
        boolean isBuyer = offer.getMakersDirection().isBuy();
        Identity myIdentity = identityService.findAnyIdentityByNetworkId(offer.getMakerNetworkId()).orElseThrow();
        MuSigTrade muSigTrade = new MuSigTrade(contract, isBuyer, false, myIdentity, offer, sender, receiver);
        String tradeId = muSigTrade.getId();
        checkArgument(findProtocol(tradeId).isEmpty(), "We received the MuSigTakeOfferRequest for an already existing protocol");
        checkArgument(!tradeExists(tradeId), "A trade with that ID exists already");
        persistableStore.addTrade(muSigTrade);
        return createAndAddTradeProtocol(muSigTrade);
    }

    private MuSigProtocol createAndAddTradeProtocol(MuSigTrade trade) {
        String id = trade.getId();
        MuSigProtocol tradeProtocol;
        boolean isBuyer = trade.isBuyer();
        if (trade.isTaker()) {
            if (isBuyer) {
                tradeProtocol = new MuSigBuyerAsTakerProtocol(serviceProvider, trade);
            } else {
                tradeProtocol = new MuSigSellerAsTakerProtocol(serviceProvider, trade);
            }
        } else {
            if (isBuyer) {
                tradeProtocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
            } else {
                tradeProtocol = new MuSigSellerAsMakerProtocol(serviceProvider, trade);
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
        if (requireVersionForTrading && minRequiredVersionForTrading.isPresent()) {
            checkArgument(ApplicationVersion.getVersion().aboveOrEqual(new Version(minRequiredVersionForTrading.get())),
                    "For trading you need to have version " + minRequiredVersionForTrading.get() + " installed. " +
                            "The Bisq security manager has published an emergency alert with a min. version required for trading.");
        }
    }


    /* --------------------------------------------------------------------- */
    // Redact sensible data
    /* --------------------------------------------------------------------- */

    private void maybeRedactDataOfCompletedTrades() {
        int numDays = settingsService.getNumDaysAfterRedactingTradeData().get();
        long redactDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(numDays);
        // Trades which ended up with a failure or got stuck will never get the completed date set.
        // We use a more constrained duration of 45-90 days.
        int numDaysForNotCompletedTrades = Math.max(45, Math.min(90, numDays));
        long redactDateForNotCompletedTrades = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(numDaysForNotCompletedTrades);
        long numChanges = getTrades().stream()
                .filter(trade -> {
                    boolean doRedaction = trade.getTradeCompletedDate().map(date -> date < redactDate)
                            .orElse(trade.getContract().getTakeOfferDate() < redactDateForNotCompletedTrades);
                    //todo
                    return doRedaction;
                })
                .count();
        if (numChanges > 0) {
            persist();
        }
    }
}