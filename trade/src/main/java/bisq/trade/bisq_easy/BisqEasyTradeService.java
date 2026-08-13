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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import static bisq.trade.bisq_easy.validation.BisqEasyOfferAmountValidator.validateOfferAmount;
import static com.google.common.base.Preconditions.checkArgument;

//TODO Consider to use async calls at handle (CompletableFuture.runAsync(()...)

@Slf4j
@Getter
public class BisqEasyTradeService extends RateLimitedPersistenceClient<BisqEasyTradeStore> implements Service, ConfidentialMessageService.Listener {
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
    // Guards makerCreatesProtocol's check-then-act creation sequence (the duplicate-protocol/duplicate-trade
    // checks, together with the writes that make the trade+protocol exist). See makerCreatesProtocol for why.
    private final Object creationLock = new Object();

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

        // Register the alert observer BEFORE restoring trades: addObserver() synchronously replays the
        // persisted authorized alert data (CollectionObserver#onAllAdded), so haltTrading /
        // requireVersionForTrading reflect the last known alert state by the time the restore drain below
        // re-applies queued events. Registered after the restore (as before), the drain-time checks would be
        // vacuously green on every cold start.
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

        persistableStore.getTrades().forEach(trade -> createAndAddTradeProtocol(trade, true));

        // Register the live listener BEFORE the startup replay below. With the reverse order there was a
        // handoff window: a message the network finished processing after the replay read its snapshot but
        // before registration was ACKed and removed from the mailbox without ever reaching the FSM - lost for
        // good. Register-first closes that window structurally, at the cost of possible duplicate delivery
        // (live + replay) - which is expected and handled: once a protocol exists, a duplicate is absorbed by
        // the FSM (processed-events filter, forward-only transition guard); before a protocol exists (a
        // duplicate take-offer request), makerCreatesProtocol's checkArguments plus creationLock guard against
        // it instead - see there. A live message arriving before its predecessor is replayed is exactly the
        // out-of-order case the FSM event queue absorbs.
        networkService.addConfidentialMessageListener(this);
        networkService.getConfidentialMessageServices().stream()
                .flatMap(service -> service.getProcessedEnvelopePayloadMessages().stream())
                .forEach(message -> {
                    // Per-message isolation: with the alert observer registered above, onMessage()'s
                    // halt/min-version guards can now legitimately throw during this startup replay (they
                    // could not before - the observer used to be registered only after this loop). A rejected
                    // replayed message must not abort initialize() and skip every subsequent message.
                    try {
                        onMessage(message);
                    } catch (Exception e) {
                        log.warn("Re-feeding a processed message at startup was rejected (e.g. trading halted " +
                                "by an emergency alert). messageType={}", message.getClass().getSimpleName(), e);
                    }
                });

        numDaysAfterRedactingTradeDataScheduler = Scheduler.run(this::maybeRedactDataOfCompletedTrades)
                .host(this)
                .periodically(1, TimeUnit.HOURS);
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
        // Duplicate delivery of the very same take-offer request is expected now that the live listener is
        // registered before the startup replay (see initialize()): the live path and the replay can both hand
        // us the same message, and a mailbox message can be redelivered too. If a protocol for this trade id
        // already exists we are looking at a duplicate of an already-completed creation, not a new offer being
        // taken - route it straight through the normal FSM path below (which absorbs it) instead of attempting
        // to create a second trade/protocol for it. The id must be derived exactly the way makerCreatesProtocol
        // derives it (single-sourced in BisqEasyTrade#createId), or this lookup could miss and fall through to
        // a redundant creation attempt.
        String tradeId = BisqEasyTrade.createId(bisqEasyContract, message.getSender());
        BisqEasyProtocol protocol = findProtocol(tradeId)
                .orElseGet(() -> makerCreatesProtocol(bisqEasyContract, message.getSender(), message.getReceiver()));
        handleBisqEasyTradeMessage(message, protocol);
    }

    private void handleBisqEasyTradeMessage(BisqEasyTradeMessage message) {
        String tradeId = message.getTradeId();
        findProtocol(tradeId).ifPresentOrElse(protocol -> handleBisqEasyTradeMessage(message, protocol),
                () -> {
                    log.info("Protocol with tradeId {} not found. We add the message to pendingMessages for " +
                            "re-processing when the next message arrives. messageType={}",
                    tradeId, message.getClass().getSimpleName());
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

    // Package-private (rather than private) so a concurrency test can drive it directly without having to
    // forge a signed take-offer request.
    BisqEasyProtocol makerCreatesProtocol(BisqEasyContract contract, NetworkId sender, NetworkId receiver) {
        // The fast path in handleBisqEasyTakeOfferMessage already routes a duplicate away from here whenever a
        // protocol for this trade id already exists - but that check is lock-free, so two deliveries for a
        // trade id that does NOT exist yet (live listener + startup replay, or two mailbox redeliveries) can
        // both reach here concurrently. Creation must be check-then-act atomic: the "no duplicate exists"
        // checks below and the writes that make the trade+protocol exist (addTrade/persist/
        // createAndAddTradeProtocol) have to happen as one unit, or both deliveries can pass the checks and
        // each create their own trade+protocol for the same id - a split brain where one of the two protocol
        // instances is silently orphaned (never reachable via findProtocol/tradeProtocolById again) and any
        // handler side effect (e.g. sending the take-offer response to the peer) runs once per created
        // protocol instead of once overall. Deliberately NOT held across protocol.handle() in the caller: that
        // takes the FSM model lock and can itself trigger a persist() call, and is already safe for a
        // duplicate once a protocol exists (forward-only transition guard / processed-events filter).
        synchronized (creationLock) {
            // We only create the data required for the protocol creation.
            // Verification will happen in the BisqEasyTakeOfferRequestHandler
            BisqEasyOffer offer = contract.getOffer();
            boolean isBuyer = offer.getMakersDirection().isBuy();
            Identity myIdentity = identityService.findAnyIdentityByNetworkId(offer.getMakerNetworkId()).orElseThrow();
            BisqEasyTrade bisqEasyTrade = new BisqEasyTrade(contract, isBuyer, false, myIdentity, offer, sender, receiver);
            String tradeId = bisqEasyTrade.getId();
            checkArgument(findProtocol(tradeId).isEmpty(), "We received the BisqEasyTakeOfferRequest for an already existing protocol");
            checkArgument(!tradeExists(tradeId), "A trade with that ID exists already");

            awaitCreationTestSeam(tradeId);

            persistableStore.addTrade(bisqEasyTrade);
            persist();

            maybeAddPeerToContactList(sender.getId(), myIdentity.getId());

            return createAndAddTradeProtocol(bisqEasyTrade);
        }
    }

    // No-op hook, overridden only in tests. Lets a concurrency test deterministically park the thread holding
    // creationLock here - between the "no duplicate exists" checks above and the writes that make the trade/
    // protocol exist - while a second thread attempts to race it, forcing the exact interleaving creationLock
    // guards against instead of relying on scheduling luck.
    void awaitCreationTestSeam(String tradeId) {
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
            // The queued events passed onMessage()'s guards when they were originally received, but those
            // conditions may have changed before the restart. Draining applies them directly through the FSM,
            // so re-check here what onMessage() would check for a live message:
            //  - Trading halt / min-version (global, temporary): defer the drain entirely and KEEP the events
            //    queued - they persist and drain on a later restart once the emergency alert is lifted.
            //    Deliberately no throw: the catch below would mislabel it "stuck until investigated".
            //  - Banned sender (per event): DROP the event, mirroring onMessage() ignoring a banned sender's
            //    live message. Kept queued it would get applied after a ban-lift restart - something
            //    onMessage() would never have allowed.
            if (haltTrading || isMinVersionForTradingViolated()) {
                log.warn("Deferring the queued-event drain for restored trade {}: an emergency alert halts " +
                        "trading or requires a min version. The events stay queued and drain on a later " +
                        "restart once the alert is lifted.", id);
            } else {
                boolean removedBannedSenderEvents = trade.removeQueuedEventsIf(event ->
                        event instanceof BisqEasyTradeMessage message &&
                                bannedUserService.isUserProfileBanned(message.getSender()));
                if (removedBannedSenderEvents) {
                    log.warn("Removed queued event(s) from a banned sender for restored trade {} before " +
                            "draining, mirroring the onMessage() banned-sender check.", id);
                    persist();
                }
                // Isolate per trade: drainEventQueue() re-applies queued events and can raise an FsmException. The
                // trade is already created and registered above, so we keep it regardless. Without this guard a
                // single failing trade would escape the persistableStore.getTrades().forEach(...) loop in
                // initialize() and block restoring every subsequent trade. A failed drain here means the trade
                // stays stuck until it is manually looked at - there is no periodic or reconnect-triggered safety
                // net to retry it.
                try {
                    tradeProtocol.drainEventQueue();
                } catch (Exception e) {
                    log.warn("Failed to drain the event queue for restored trade {} on load. The trade is still " +
                            "loaded but remains stuck until manually investigated.", id, e);
                }
            }
        }
        return tradeProtocol;
    }

    private void verifyTradingNotOnHalt() {
        checkArgument(!haltTrading, "Trading is on halt for security reasons. " +
                "The Bisq security manager has published an emergency alert with haltTrading set to true");
    }

    private void verifyMinVersionForTrading() {
        checkArgument(!isMinVersionForTradingViolated(),
                "For trading you need to have version " + minRequiredVersionForTrading.orElse("") + " installed. " +
                        "The Bisq security manager has published an emergency alert with a min. version required for trading.");
    }

    private boolean isMinVersionForTradingViolated() {
        return requireVersionForTrading && minRequiredVersionForTrading.isPresent() &&
                !ApplicationVersion.getVersion().aboveOrEqual(new Version(minRequiredVersionForTrading.get()));
    }


    /* --------------------------------------------------------------------- */
    // Redact sensible data
    /* --------------------------------------------------------------------- */

    // Package-private (rather than private) so tests can drive the retention pass deterministically instead of
    // waiting on the periodic scheduler.
    void maybeRedactDataOfCompletedTrades() {
        int numDays = settingsService.getNumDaysAfterRedactingTradeData().get();
        long redactDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(numDays);
        // Trades which ended up with a failure or got stuck will never get the completed date set.
        // We use a more constrained duration of 45-90 days.
        int numDaysForNotCompletedTrades = Math.max(45, Math.min(90, numDays));
        long redactDateForNotCompletedTrades = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(numDaysForNotCompletedTrades);
        long numChanges = getAllTrades().stream()
                .filter(trade -> {
                    boolean doRedaction = trade.getTradeCompletedDate().map(date -> date < redactDate)
                            .orElseGet(() -> trade.getContract().getTakeOfferDate() < redactDateForNotCompletedTrades);
                    if (!doRedaction) {
                        return false;
                    }
                    boolean changed = false;
                    String paymentAccountData = trade.getPaymentAccountData().get();
                    if (!StringUtils.isEmpty(paymentAccountData)) {
                        // Resolve the marker lazily (only when there is account data to redact) so the pass does no
                        // i18n lookup for trades that just need their pending event queue scrubbed.
                        String redactedMarker = Res.get("data.redacted");
                        if (!paymentAccountData.equals(redactedMarker)) {
                            trade.getPaymentAccountData().set(redactedMarker);
                            changed = true;
                        }
                    }
                    // Out-of-order events persist the full account-data network message (BisqEasyAccountDataMessage);
                    // a stuck trade may never reach a final state to clear them, so scrub them on the same retention
                    // threshold rather than leaving sensitive data on disk indefinitely.
                    if (!trade.getEventQueue().isEmpty()) {
                        trade.clearEventQueue();
                        changed = true;
                    }
                    return changed;
                })
                .count();
        if (numChanges > 0) {
            // Sensitive data was scrubbed in-memory; route through persistNow() so the write can't be silently
            // dropped by the rate limiter and leave it on disk (same rationale as the final-state clear).
            persistNow();
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
