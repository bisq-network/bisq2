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

import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.fsm.Event;
import bisq.common.monetary.Monetary;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.common.platform.Version;
import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.PriceSpec;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.settings.SettingsService;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyBuyerAsMakerProtocol;
import bisq.trade.bisq_easy.protocol.BisqEasyBuyerAsTakerProtocol;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.trade.bisq_easy.protocol.BisqEasySellerAsMakerProtocol;
import bisq.trade.bisq_easy.protocol.BisqEasySellerAsTakerProtocol;
import bisq.trade.bisq_easy.protocol.events.BisqEasyAccountDataEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyBtcConfirmedEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyCancelTradeEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyConfirmBtcSentEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyConfirmFiatReceiptEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyConfirmFiatSentEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyRejectTradeEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasySendBtcAddressEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyTakeOfferEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyTradeEvent;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;
import bisq.user.banned.BannedUserService;
import bisq.user.contact_list.ContactListService;
import bisq.user.contact_list.ContactReason;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static bisq.trade.bisq_easy.validation.BisqEasyOfferAmountValidator.validateOfferAmount;
import static com.google.common.base.Preconditions.checkArgument;

//TODO Consider to use async calls at handle (CompletableFuture.runAsync(()...)

@Slf4j
@Getter
public class BisqEasyTradeService extends RateLimitedPersistenceClient<BisqEasyTradeStore> implements Service, ConfidentialMessageService.Listener {
    // Debounce window applied after a transport node reaches RUNNING before we run the recovery pass. Mirrors
    // bisq.network.p2p.services.confidential.resend.ResendMessageService's identical pattern/rationale: right
    // after a (re)connect a burst of node-state changes and redelivered messages arrive in quick succession, so
    // we wait briefly for things to settle instead of racing them with a recovery pass.
    private static final long RECOVER_STALLED_TRADES_RECONNECT_DEBOUNCE_SEC = 10;
    // Periodic safety net in case no reconnect event ever fires while a trade is nonetheless stalled (e.g. a
    // long-lived connection during which a message was received but, for whatever reason, never actually reached
    // the FSM). BisqEasy trades are human-paced (fiat transfer confirmations take minutes to days, not seconds),
    // and each pass is cheap - an in-memory filter plus, for every trade that is not actually stuck, a handful of
    // FSM no-ops - so a few minutes bounds the worst-case recovery latency without meaningful CPU/log overhead.
    private static final long RECOVER_STALLED_TRADES_PERIODIC_INTERVAL_MIN = 5;

    private final ServiceProvider serviceProvider;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final SettingsService settingsService;
    private final BannedUserService bannedUserService;
    private final AlertService alertService;

    private final Persistence<BisqEasyTradeStore> persistence;
    private final AppType appType;
    private final BisqEasyTradeStore persistableStore = new BisqEasyTradeStore();

    // We don't persist the protocol, only the model.
    private final Map<String, BisqEasyProtocol> tradeProtocolById = new ConcurrentHashMap<>();
    private final ContactListService contactListService;
    private final UserProfileService userProfileService;
    private boolean haltTrading;
    private boolean requireVersionForTrading;
    private Optional<String> minRequiredVersionForTrading = Optional.empty();
    @Nullable
    private Pin authorizedAlertDataSetPin, numDaysAfterRedactingTradeDataPin;
    @Nullable
    private Scheduler numDaysAfterRedactingTradeDataScheduler;
    private final Set<BisqEasyTradeMessage> pendingMessages = new CopyOnWriteArraySet<>();
    private final Set<Pin> recoverStalledTradesNodeStatePins = new HashSet<>();
    @Nullable
    private Scheduler recoverStalledTradesReconnectScheduler;
    @Nullable
    private Scheduler recoverStalledTradesPeriodicScheduler;

    public BisqEasyTradeService(ServiceProvider serviceProvider, AppType appType) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        settingsService = serviceProvider.getSettingsService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        alertService = serviceProvider.getBondedRolesService().getAlertService();
        contactListService = serviceProvider.getUserService().getContactListService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();

        persistence = serviceProvider.getPersistenceService().getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.appType = appType;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {

        persistableStore.getTrades().forEach(trade -> createAndAddTradeProtocol(trade, true));

        networkService.getConfidentialMessageServices().stream()
                .flatMap(service -> service.getProcessedEnvelopePayloadMessages().stream())
                .forEach(this::onMessage);
        networkService.addConfidentialMessageListener(this);

        authorizedAlertDataSetPin = alertService.getAuthorizedAlertDataSet().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(AuthorizedAlertData authorizedAlertData) {
                if (authorizedAlertData.getAlertType() == AlertType.EMERGENCY && authorizedAlertData.getAppType() == appType) {
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
            public void onRemoved(Object element) {
                if (element instanceof AuthorizedAlertData authorizedAlertData) {
                    if (authorizedAlertData.getAlertType() == AlertType.EMERGENCY && authorizedAlertData.getAppType() == appType) {
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
            public void onCleared() {
                haltTrading = false;
                requireVersionForTrading = false;
                minRequiredVersionForTrading = Optional.empty();
            }
        });

        numDaysAfterRedactingTradeDataScheduler = Scheduler.run(this::maybeRedactDataOfCompletedTrades)
                .host(this)
                .periodically(1, TimeUnit.HOURS);
        numDaysAfterRedactingTradeDataPin = settingsService.getNumDaysAfterRedactingTradeData().addObserver(numDays -> maybeRedactDataOfCompletedTrades());

        initRecoverStalledTrades();

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
        recoverStalledTradesNodeStatePins.forEach(Pin::unbind);
        recoverStalledTradesNodeStatePins.clear();
        if (recoverStalledTradesReconnectScheduler != null) {
            recoverStalledTradesReconnectScheduler.stop();
            recoverStalledTradesReconnectScheduler = null;
        }
        if (recoverStalledTradesPeriodicScheduler != null) {
            recoverStalledTradesPeriodicScheduler.stop();
            recoverStalledTradesPeriodicScheduler = null;
        }

        networkService.removeConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof BisqEasyTradeMessage bisqEasyTradeMessage) {
            verifyTradingNotOnHalt();
            verifyMinVersionForTrading();

            if (bannedUserService.isUserProfileBanned(bisqEasyTradeMessage.getSender())) {
                log.warn("Message ignored as sender is banned");
                return;
            }

            if (bisqEasyTradeMessage instanceof BisqEasyTakeOfferRequest) {
                handleBisqEasyTakeOfferMessage((BisqEasyTakeOfferRequest) bisqEasyTradeMessage);
            } else {
                handleBisqEasyTradeMessage(bisqEasyTradeMessage);
            }
        }
    }


    /* --------------------------------------------------------------------- */
    // Message event
    /* --------------------------------------------------------------------- */

    private void handleBisqEasyTakeOfferMessage(BisqEasyTakeOfferRequest message) {
        BisqEasyContract bisqEasyContract = message.getBisqEasyContract();
        BisqEasyProtocol protocol = makerCreatesProtocol(bisqEasyContract, message.getSender(), message.getReceiver());
        handleBisqEasyTradeMessage(message, protocol);
    }

    private void handleBisqEasyTradeMessage(BisqEasyTradeMessage message) {
        String tradeId = message.getTradeId();
        findProtocol(tradeId).ifPresentOrElse(protocol -> handleBisqEasyTradeMessage(message, protocol),
                () -> {
                    log.info("Protocol with tradeId {} not found. We add the message to pendingMessages for " +
                            "re-processing when the next message arrives. message={}", tradeId, message);
                    pendingMessages.add(message);
                });
    }

    private void handleBisqEasyTradeMessage(BisqEasyTradeMessage message, BisqEasyProtocol protocol) {
        protocol.handle(message);

        if (pendingMessages.contains(message)) {
            log.info("We remove message {} from pendingMessages.", message);
            pendingMessages.remove(message);
        }

        if (!pendingMessages.isEmpty()) {
            log.info("We have pendingMessages. We try to re-process them now.");
            pendingMessages.forEach(this::handleBisqEasyTradeMessage);
        }
    }


    /* --------------------------------------------------------------------- */
    // Events
    /* --------------------------------------------------------------------- */

    public BisqEasyProtocol takerCreatesProtocol(Identity takerIdentity,
                                                 BisqEasyOffer bisqEasyOffer,
                                                 Monetary baseSideAmount,
                                                 Monetary quoteSideAmount,
                                                 BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec,
                                                 FiatPaymentMethodSpec fiatPaymentMethodSpec,
                                                 Optional<UserProfile> mediator,
                                                 PriceSpec priceSpec,
                                                 long marketPrice) {
        verifyTradingNotOnHalt();
        verifyMinVersionForTrading();
        validateOfferAmount(bisqEasyOffer, baseSideAmount.getValue(), quoteSideAmount.getValue());

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
                priceSpec,
                marketPrice);
        boolean isBuyer = bisqEasyOffer.getTakersDirection().isBuy();
        NetworkId makerNetworkId = contract.getMaker().getNetworkId();
        BisqEasyTrade bisqEasyTrade = new BisqEasyTrade(contract, isBuyer, true, takerIdentity, bisqEasyOffer, takerNetworkId, makerNetworkId);

        checkArgument(findProtocol(bisqEasyTrade.getId()).isEmpty(),
                "We received the BisqEasyTakeOfferRequest for an already existing protocol");

        checkArgument(!tradeExists(bisqEasyTrade.getId()), "A trade with that ID exists already");
        persistableStore.addTrade(bisqEasyTrade);
        persist();

        maybeAddPeerToContactList(makerNetworkId.getId(), takerNetworkId.getId());

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
        String tradeId = trade.getId();
        findProtocol(tradeId).ifPresentOrElse(protocol -> protocol.handle(event),
                () -> log.info("Protocol with tradeId {} not found. This is expected if the trade have been closed already", tradeId));
    }


    /* --------------------------------------------------------------------- */
    // Misc API
    /* --------------------------------------------------------------------- */

    public Optional<BisqEasyProtocol> findProtocol(String id) {
        return Optional.ofNullable(tradeProtocolById.get(id));
    }

    public Optional<BisqEasyTrade> findTrade(String tradeId) {
        return persistableStore.findTrade(tradeId);
    }

    public boolean tradeExists(String tradeId) {
        return persistableStore.tradeExists(tradeId);
    }

    public boolean wasOfferAlreadyTaken(BisqEasyOffer bisqEasyOffer, NetworkId takerNetworkId) {
        return getTrades().stream().anyMatch(trade ->
                trade.getOffer().getId().equals(bisqEasyOffer.getId()) &&
                        trade.getTaker().getNetworkId().getId().equals(takerNetworkId.getId())
        );
    }

    public ReadOnlyObservableSet<BisqEasyTrade> getTrades() {
        return persistableStore.getTrades();
    }

    public ReadOnlyObservableSet<BisqEasyTrade> getAllTrades() {
        return persistableStore.getAllTrades();
    }

    public ReadOnlyObservableSet<BisqEasyClosedTrade> getClosedTrades() {
        return persistableStore.getClosedTrades();
    }

    public void closeTrade(BisqEasyTrade trade, UserProfile myUserProfile, UserProfile peerUserProfile) {
        persistableStore.getTrades().remove(trade);
        BisqEasyClosedTrade bisqEasyClosedTrade = new BisqEasyClosedTrade(trade, myUserProfile, peerUserProfile);
        persistableStore.getClosedTrades().add(bisqEasyClosedTrade);

        tradeProtocolById.remove(trade.getId());
        persist();
    }

    public void deleteTrade(BisqEasyTrade trade) {
        Set<BisqEasyClosedTrade> closedTrades = persistableStore.getClosedTrades();
        Optional<BisqEasyClosedTrade> closedTrade = closedTrades.stream()
                .filter(ct -> ct.trade().getId().equals(trade.getId()))
                .findFirst();
        if (closedTrade.isPresent()) {
            closedTrades.remove(closedTrade.get());
            persist();
        } else {
            log.warn("Could not delete trade {}", trade.getId());
        }
    }


    /* --------------------------------------------------------------------- */
    // TradeProtocol factory
    /* --------------------------------------------------------------------- */

    private BisqEasyProtocol makerCreatesProtocol(BisqEasyContract contract, NetworkId sender, NetworkId receiver) {
        // We only create the data required for the protocol creation.
        // Verification will happen in the BisqEasyTakeOfferRequestHandler
        BisqEasyOffer offer = contract.getOffer();
        boolean isBuyer = offer.getMakersDirection().isBuy();
        Identity myIdentity = identityService.findAnyIdentityByNetworkId(offer.getMakerNetworkId()).orElseThrow();
        BisqEasyTrade bisqEasyTrade = new BisqEasyTrade(contract, isBuyer, false, myIdentity, offer, sender, receiver);
        String tradeId = bisqEasyTrade.getId();
        checkArgument(findProtocol(tradeId).isEmpty(), "We received the BisqEasyTakeOfferRequest for an already existing protocol");
        checkArgument(!tradeExists(tradeId), "A trade with that ID exists already");

        persistableStore.addTrade(bisqEasyTrade);
        persist();

        maybeAddPeerToContactList(sender.getId(), myIdentity.getId());

        return createAndAddTradeProtocol(bisqEasyTrade);
    }

    private BisqEasyProtocol createAndAddTradeProtocol(BisqEasyTrade trade) {
        return createAndAddTradeProtocol(trade, false);
    }

    // isRestoredTrade is true when the trade was just loaded from persisted data (app startup), as opposed to a
    // trade which was just created for a brand-new offer/take-offer flow (whose event queue is always empty).
    // For restored trades we drain the event queue once: the queue itself survives a restart (persisted on
    // Trade), but nothing would otherwise re-attempt those pending events until some further, unrelated live
    // transition happens to occur for that same trade - which may never happen. See bisq.common.fsm.Fsm#drainEventQueue.
    // Package-private (rather than private) so tests can register a hand-placed trade's protocol exactly the way
    // production code does, without duplicating this wiring.
    BisqEasyProtocol createAndAddTradeProtocol(BisqEasyTrade trade, boolean isRestoredTrade) {
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
        if (isRestoredTrade) {
            // Isolate per trade: drainEventQueue() re-applies queued events and can raise an FsmException. The
            // trade is already created and registered above, so we keep it and let the periodic/reconnect
            // recovery pass retry. Without this guard a single failing trade would escape the
            // persistableStore.getTrades().forEach(...) loop in initialize() and block restoring every
            // subsequent trade - consistent with the per-trade isolation in reprocessTrade()/recoverStalledTrades().
            try {
                tradeProtocol.drainEventQueue();
            } catch (Exception e) {
                log.warn("Failed to drain the event queue for restored trade {} on load. The trade is still " +
                        "loaded; the recovery pass will retry.", id, e);
            }
        }
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
    // Live recovery pass (#1622 follow-up)
    /* --------------------------------------------------------------------- */

    // See bisq.common.fsm.Fsm#drainEventQueue: a queued event only gets re-attempted after some *further* live
    // transition happens to occur on that same trade, which may never happen. drainEventQueue() alone therefore
    // cannot fix a trade where the enabling message was received but, for whatever reason (a transient bug, a
    // service that was not yet registered as a listener at the time, ...), never even reached the FSM in the
    // first place - there is nothing queued to drain. This recovery pass additionally re-feeds such messages from
    // the confidential-message layer's own live, in-memory record of everything it has decrypted this session
    // (ConfidentialMessageService#getProcessedEnvelopePayloadMessages, which is not scoped to "not yet applied" -
    // see reprocessTrade for how we filter it safely), so it can recover the trade without needing a restart or a
    // fresh redelivery from the peer.
    private void initRecoverStalledTrades() {
        networkService.getDefaultNodeStateByTransportType().forEach((transportType, nodeState) -> {
            Pin pin = nodeState.addObserver(state -> {
                if (state == Node.State.RUNNING) {
                    if (recoverStalledTradesReconnectScheduler != null) {
                        recoverStalledTradesReconnectScheduler.stop();
                    }
                    recoverStalledTradesReconnectScheduler = Scheduler.run(this::recoverStalledTrades)
                            .host(this)
                            .runnableName("recoverStalledTradesOnReconnect")
                            .after(RECOVER_STALLED_TRADES_RECONNECT_DEBOUNCE_SEC, TimeUnit.SECONDS);
                }
            });
            recoverStalledTradesNodeStatePins.add(pin);
        });

        // Periodic safety net; the reconnect-triggered pass above is the primary trigger. Delay the first run by
        // the same interval so we do not duplicate the work initialize() already did a moment ago (persisted-trade
        // protocol restore + the one-off processed-message replay above).
        recoverStalledTradesPeriodicScheduler = Scheduler.run(this::recoverStalledTrades)
                .host(this)
                .runnableName("recoverStalledTradesPeriodic")
                .periodically(RECOVER_STALLED_TRADES_PERIODIC_INTERVAL_MIN, RECOVER_STALLED_TRADES_PERIODIC_INTERVAL_MIN, TimeUnit.MINUTES);
    }

    // Must never throw: this runs both from a Scheduler's Runnable (an uncaught exception would silently stop all
    // future periodic executions - see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay) and
    // from a node-state observer callback. Per-trade failures are additionally isolated in reprocessTrade itself,
    // so this outer guard is defense in depth against a failure in the iteration/filtering step itself.
    private void recoverStalledTrades() {
        try {
            List<BisqEasyTrade> openTrades = getTrades().stream()
                    .filter(trade -> !trade.getTradeState().isFinalState())
                    .collect(Collectors.toList());
            int numTradesReprocessed = 0;
            int numMessagesReapplied = 0;
            for (BisqEasyTrade trade : openTrades) {
                int reapplied = reprocessTrade(trade);
                if (reapplied > 0) {
                    numTradesReprocessed++;
                    numMessagesReapplied += reapplied;
                }
            }
            if (numTradesReprocessed > 0) {
                log.info("Recovery pass reprocessed {} stalled trade(s) (re-applied {} message(s) in total) out of {} open trade(s) checked.",
                        numTradesReprocessed, numMessagesReapplied, openTrades.size());
            } else {
                log.debug("Recovery pass checked {} open trade(s); none needed reprocessing.", openTrades.size());
            }
        } catch (Exception e) {
            log.error("Unexpected error during recoverStalledTrades. This pass is aborted, but the next " +
                    "scheduled/reconnect-triggered pass is unaffected.", e);
        }
    }

    /**
     * Re-applies any of the given trade's already-received-but-not-yet-applied protocol messages, then drains the
     * FSM's event queue. Package-private so it can also be invoked directly (e.g. from tests, or a future manual
     * "unstick this trade" UI action) without going through the full {@link #recoverStalledTrades()} sweep.
     * <br/>
     * Safety properties this relies on (see bisq.common.fsm.Fsm#handle):
     * <ul>
     *     <li>Messages are selected only if their event class is not already in the trade's processedEvents,
     *     which is populated the moment a transition for that class first succeeds. This is what makes the
     *     re-feed idempotent, and is also why {@code BisqEasyTakeOfferRequest}/{@code BisqEasyTakeOfferResponse}
     *     (the offer-negotiation messages) are automatically excluded for any trade reaching this method: their
     *     transition necessarily already fired for a protocol/trade to exist at all.</li>
     *     <li>We call {@code protocol.handle(message)} directly - never {@link #onMessage(EnvelopePayloadMessage)}
     *     or the private take-offer routing behind it. Routing a {@code BisqEasyTakeOfferRequest} back through
     *     {@code onMessage()} would call {@code makerCreatesProtocol()} again, which throws
     *     {@code IllegalArgumentException} because the protocol/trade already exists for any trade we reprocess -
     *     the processedEvents filter above prevents that message from ever being selected in the first place, but
     *     this method still never uses that entry point, as a second line of defense.</li>
     *     <li>{@code Fsm#handle} and {@code Fsm#drainEventQueue} are both {@code synchronized} on the per-trade
     *     protocol instance, so this is safe to run concurrently with a genuine live inbound message for the same
     *     trade (mutual exclusion on the existing per-trade lock; no new shared mutable state is introduced).</li>
     * </ul>
     *
     * @return the number of messages that were re-applied to the trade's FSM (0 if nothing was pending, the trade
     * has no registered protocol, or the trade already reached a final state).
     */
    int reprocessTrade(BisqEasyTrade trade) {
        String tradeId = trade.getId();
        if (trade.getTradeState().isFinalState()) {
            // Defensive: the trade may have reached a final state concurrently with (or just before) this call,
            // e.g. a live transition completing it on another thread between recoverStalledTrades()'s filter and
            // this call. processedEvents is cleared once a final state is reached (see Fsm#handle), so selecting
            // messages against an emptied processedEvents set below would otherwise look exactly like "nothing was
            // ever applied" and re-select everything, including the original take-offer message. Fsm#handle would
            // still no-op it via its own isFinalState guard, but there is no reason to do the work.
            log.debug("Skipping reprocessTrade for trade {} as it already reached the final state {}.", tradeId, trade.getTradeState());
            return 0;
        }

        Optional<BisqEasyProtocol> optionalProtocol = findProtocol(tradeId);
        if (optionalProtocol.isEmpty()) {
            log.debug("Skipping reprocessTrade for trade {} as no protocol is registered for it (yet).", tradeId);
            return 0;
        }
        BisqEasyProtocol protocol = optionalProtocol.get();

        try {
            Set<Class<? extends Event>> processedEvents = trade.getProcessedEvents();
            List<BisqEasyTradeMessage> candidateMessages = networkService.getConfidentialMessageServices().stream()
                    .flatMap(confidentialMessageService -> confidentialMessageService.getProcessedEnvelopePayloadMessages().stream())
                    .filter(BisqEasyTradeMessage.class::isInstance)
                    .map(BisqEasyTradeMessage.class::cast)
                    .filter(message -> tradeId.equals(message.getTradeId()))
                    .filter(message -> !processedEvents.contains(message.getClass()))
                    .collect(Collectors.toList());

            int numApplied = 0;
            for (BisqEasyTradeMessage message : candidateMessages) {
                protocol.handle(message);
                numApplied++;
                log.info("Recovery pass re-applied {} to trade {}.", message.getClass().getSimpleName(), tradeId);
            }

            // Whether or not any message above triggered a transition, draining is cheap and safe (no-op on an
            // empty queue), and covers the case where the newly-applied message unblocked a *different*,
            // previously queued event rather than one we just re-fed directly.
            protocol.drainEventQueue();

            if (numApplied > 0) {
                log.info("Recovery pass reprocessed trade {}: re-applied {} message(s), resulting state={}.",
                        tradeId, numApplied, trade.getTradeState());
            }
            return numApplied;
        } catch (Exception e) {
            log.warn("Recovery pass failed to reprocess trade {}. Leaving it for the next pass.", tradeId, e);
            return 0;
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
        String redactedMarker = Res.get("data.redacted");
        long numChanges = getAllTrades().stream()
                .filter(trade -> {
                    if (StringUtils.isEmpty(trade.getPaymentAccountData().get()) ||
                            trade.getPaymentAccountData().get().equals(redactedMarker)) {
                        return false;
                    }
                    boolean doRedaction = trade.getTradeCompletedDate().map(date -> date < redactDate)
                            .orElseGet(() -> trade.getContract().getTakeOfferDate() < redactDateForNotCompletedTrades);
                    if (doRedaction) {

                        trade.getPaymentAccountData().set(redactedMarker);
                    }
                    return doRedaction;
                })
                .count();
        if (numChanges > 0) {
            persist();
        }
    }


    /* --------------------------------------------------------------------- */
    // Misc
    /* --------------------------------------------------------------------- */

    private void maybeAddPeerToContactList(String peersProfileId, String myProfileId) {
        if (settingsService.getDoAutoAddToContactList()) {
            Optional<UserProfile> peersProfile = userProfileService.findUserProfile(peersProfileId);
            Optional<UserProfile> myProfile = userProfileService.findUserProfile(myProfileId);
            if (peersProfile.isPresent() && myProfile.isPresent()) {
                contactListService.addContactListEntry(peersProfile.get(), myProfile.get(), ContactReason.BISQ_EASY_TRADE);
            }
        }
    }
}
