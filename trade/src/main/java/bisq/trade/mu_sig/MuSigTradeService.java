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

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.monetary.Monetary;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.platform.Version;
import bisq.common.timer.Scheduler;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.settings.SettingsService;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import bisq.trade.mu_sig.events.MuSigTradeEvent;
import bisq.trade.mu_sig.events.blockchain.DepositTxConfirmedEvent;
import bisq.trade.mu_sig.events.buyer.PaymentInitiatedEvent;
import bisq.trade.mu_sig.events.seller.PaymentReceiptConfirmedEvent;
import bisq.trade.mu_sig.events.taker.MuSigTakeOfferEvent;
import bisq.trade.mu_sig.grpc.MusigGrpcClient;
import bisq.trade.mu_sig.messages.grpc.TxConfirmationStatus;
import bisq.trade.mu_sig.messages.network.MuSigTradeMessage;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.protocol.MuSigBuyerAsMakerProtocol;
import bisq.trade.mu_sig.protocol.MuSigBuyerAsTakerProtocol;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.trade.mu_sig.protocol.MuSigSellerAsMakerProtocol;
import bisq.trade.mu_sig.protocol.MuSigSellerAsTakerProtocol;
import bisq.trade.protobuf.MusigGrpc;
import bisq.trade.protobuf.SubscribeTxConfirmationStatusRequest;
import bisq.user.banned.BannedUserService;
import bisq.user.contact_list.ContactListService;
import bisq.user.contact_list.ContactReason;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import io.grpc.stub.StreamObserver;
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
public final class MuSigTradeService implements PersistenceClient<MuSigTradeStore>, Service, ConfidentialMessageService.Listener {
    @Getter
    public static class Config {
        private final String host;
        private final int port;

        public Config(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public static Config from(com.typesafe.config.Config config) {
            com.typesafe.config.Config grpcServer = config.getConfig("grpcServer");
            return new Config(grpcServer.getString("host"),
                    grpcServer.getInt("port"));
        }
    }

    private final ServiceProvider serviceProvider;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final SettingsService settingsService;
    private final BannedUserService bannedUserService;
    private final AlertService alertService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final ContactListService contactListService;
    private final UserProfileService userProfileService;

    @Getter
    private final MuSigTradeStore persistableStore = new MuSigTradeStore();
    @Getter
    private final Persistence<MuSigTradeStore> persistence;
    @Getter
    private final MusigGrpcClient musigGrpcClient;

    // We don't persist the protocol, only the model.
    private final Map<String, MuSigProtocol> tradeProtocolById = new ConcurrentHashMap<>();

    private boolean haltTrading;
    private boolean requireVersionForTrading;
    private Optional<String> minRequiredVersionForTrading = Optional.empty();

    private Pin authorizedAlertDataSetPin, numDaysAfterRedactingTradeDataPin;
    private Scheduler numDaysAfterRedactingTradeDataScheduler;
    private final Set<MuSigTradeMessage> pendingMessages = new CopyOnWriteArraySet<>();
    private final Map<String, Scheduler> closeTradeTimeoutSchedulerByTradeId = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> observeDepositTxConfirmationStatusFutureByTradeId = new ConcurrentHashMap<>();

    public MuSigTradeService(Config config, ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        settingsService = serviceProvider.getSettingsService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        alertService = serviceProvider.getBondedRolesService().getAlertService();
        muSigOpenTradeChannelService = serviceProvider.getChatService().getMuSigOpenTradeChannelService();
        contactListService = serviceProvider.getUserService().getContactListService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();

        persistence = serviceProvider.getPersistenceService().getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);

        musigGrpcClient = new MusigGrpcClient(config.getHost(), config.getPort());
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        return musigGrpcClient.initialize()
                .thenApply(result -> {
                    persistableStore.getTrades().forEach(this::createAndAddTradeProtocol);

                    networkService.getConfidentialMessageServices().stream()
                            .flatMap(service -> service.getProcessedEnvelopePayloadMessages().stream())
                            .forEach(this::onMessage);
                    networkService.addConfidentialMessageListener(this);

                    // At startup we observe all unconfirmed deposit txs
                    getTrades().stream()
                            .filter(MuSigTrade::isDepositTxCreatedButNotConfirmed)
                            .forEach(this::observeDepositTxConfirmationStatus);

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

                    numDaysAfterRedactingTradeDataScheduler = Scheduler.run(this::maybeRedactDataOfCompletedTrades)
                            .host(this)
                            .periodically(1, TimeUnit.HOURS);
                    numDaysAfterRedactingTradeDataPin = settingsService.getNumDaysAfterRedactingTradeData().addObserver(numDays -> maybeRedactDataOfCompletedTrades());
                    return true;
                });
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

        observeDepositTxConfirmationStatusFutureByTradeId.values().forEach(future -> future.cancel(true));
        observeDepositTxConfirmationStatusFutureByTradeId.clear();

        networkService.removeConfidentialMessageListener(this);

        closeTradeTimeoutSchedulerByTradeId.values().forEach(Scheduler::stop);
        closeTradeTimeoutSchedulerByTradeId.clear();

        tradeProtocolById.clear();
        pendingMessages.clear();

        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MuSigTradeMessage muSigTradeMessage) {
            verifyTradingNotOnHalt();
            verifyMinVersionForTrading();

            if (bannedUserService.isUserProfileBanned(muSigTradeMessage.getSender())) {
                log.warn("Message ignored as sender is banned");
                return;
            }

            if (muSigTradeMessage instanceof SetupTradeMessage_A) {
                handleMuSigTakeOfferMessage((SetupTradeMessage_A) muSigTradeMessage);
            } else {
                handleMuSigTradeMessage(muSigTradeMessage);
            }
        }
    }


    /* --------------------------------------------------------------------- */
    // Message event
    /* --------------------------------------------------------------------- */

    private void handleMuSigTakeOfferMessage(SetupTradeMessage_A message) {
        MuSigContract muSigContract = message.getContract();
        MuSigProtocol protocol = makerCreatesProtocol(muSigContract, message.getSender(), message.getReceiver());
        handleMuSigTradeMessage(message, protocol);
    }

    private void handleMuSigTradeMessage(MuSigTradeMessage message) {
        String tradeId = message.getTradeId();
        findProtocol(tradeId).ifPresentOrElse(protocol -> handleMuSigTradeMessage(message, protocol),
                () -> {
                    log.info("Protocol with tradeId {} not found. We add the message to pendingMessages for " +
                            "re-processing when the next message arrives. message={}", tradeId, message);
                    pendingMessages.add(message);
                });
    }

    private void handleMuSigTradeMessage(MuSigTradeMessage message, MuSigProtocol protocol) {
        CompletableFuture.runAsync(() -> {
            protocol.handle(message);

            if (pendingMessages.contains(message)) {
                log.info("We remove message {} from pendingMessages.", message);
                pendingMessages.remove(message);
            }

            if (!pendingMessages.isEmpty()) {
                log.info("We have pendingMessages. We try to re-process them now.");
                pendingMessages.forEach(this::handleMuSigTradeMessage);
            }
        });
    }


    /* --------------------------------------------------------------------- */
    // User events
    /* --------------------------------------------------------------------- */

    public void takeOffer(MuSigTrade trade) {
        handleMuSigTradeEvent(trade, new MuSigTakeOfferEvent());
    }

    // TODO just temp for dev
    public void skipWaitForConfirmation(MuSigTrade trade) {
        handleMuSigTradeEvent(trade, new DepositTxConfirmedEvent());
    }

    public void paymentInitiated(MuSigTrade trade) {
        handleMuSigTradeEvent(trade, new PaymentInitiatedEvent());
    }

    public void paymentReceiptConfirmed(MuSigTrade trade) {
        handleMuSigTradeEvent(trade, new PaymentReceiptConfirmedEvent());
    }

    public void closeTrade(MuSigTrade trade) {
        //todo: just temp, we will move it to closed trades in future
        removeTrade(trade);
    }

    public void removeTrade(MuSigTrade trade) {
        persistableStore.removeTrade(trade.getId());
        tradeProtocolById.remove(trade.getId());
        persist();
    }

    private void handleMuSigTradeEvent(MuSigTrade trade, MuSigTradeEvent event) {
        verifyTradingNotOnHalt();
        verifyMinVersionForTrading();
        String tradeId = trade.getId();
        findProtocol(tradeId).ifPresentOrElse(protocol -> {
                    CompletableFuture.runAsync(() -> protocol.handle(event));
                },
                () -> log.info("Protocol with tradeId {} not found. This is expected if the trade have been closed already", tradeId));
    }


    /* --------------------------------------------------------------------- */
    // Setup
    /* --------------------------------------------------------------------- */

    public MuSigProtocol takerCreatesProtocol(Identity takerIdentity,
                                              MuSigOffer muSigOffer,
                                              Monetary baseSideAmount,
                                              Monetary quoteSideAmount,
                                              PaymentMethodSpec<?> paymentMethodSpec,
                                              AccountPayload<?> takersAccountPayload,
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
                paymentMethodSpec,
                mediator,
                priceSpec,
                marketPrice);
        boolean isBuyer = muSigOffer.getTakersDirection().isBuy();
        NetworkId makerNetworkId = contract.getMaker().getNetworkId();
        MuSigTrade muSigTrade = new MuSigTrade(contract, isBuyer, true, takerIdentity, muSigOffer, takerNetworkId, makerNetworkId);
        checkArgument(findProtocol(muSigTrade.getId()).isEmpty(),
                "We received the MuSigTakeOfferRequest for an already existing protocol");

        muSigTrade.getMyself().setAccountPayload(takersAccountPayload);

        checkArgument(!tradeExists(muSigTrade.getId()), "A trade with that ID exists already");
        persistableStore.addTrade(muSigTrade);
        persist();

        maybeAddPeerToContactList(makerNetworkId.getId());

        return createAndAddTradeProtocol(muSigTrade);
    }

    public void observeDepositTxConfirmationStatus(MuSigTrade trade) {
        // TODO Ignore the mocked confirmations and use the skip button for better control at development
        if (true) {
            return;
        }

        String tradeId = trade.getId();
        if (observeDepositTxConfirmationStatusFutureByTradeId.containsKey(tradeId)) {
            return;
        }

        // todo we dont want to create a thread for each trade... but lets see how real impl. will look like
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            SubscribeTxConfirmationStatusRequest request = SubscribeTxConfirmationStatusRequest.newBuilder()
                    .setTradeId(tradeId)
                    .build();
            getMusigAsyncStub().subscribeTxConfirmationStatus(request, new StreamObserver<>() {
                @Override
                public void onNext(bisq.trade.protobuf.TxConfirmationStatus proto) {
                    TxConfirmationStatus status = TxConfirmationStatus.fromProto(proto);
                    if (status.getNumConfirmations() > 0) {
                        if (trade.isDepositTxCreatedButNotConfirmed()) {
                            handleMuSigTradeEvent(trade, new DepositTxConfirmedEvent());
                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onCompleted() {
                }
            });
        });
        observeDepositTxConfirmationStatusFutureByTradeId.put(tradeId, future);
    }

    public void startCloseTradeTimeout(MuSigTrade trade, MuSigTradeEvent event) {
        stopCloseTradeTimeout(trade);
        closeTradeTimeoutSchedulerByTradeId.computeIfAbsent(trade.getId(), key ->
                Scheduler.run(() ->
                                handleMuSigTradeEvent(trade, event))
                        .after(24, TimeUnit.HOURS));
    }

    public void stopCloseTradeTimeout(MuSigTrade trade) {
        String tradeId = trade.getId();
        if (closeTradeTimeoutSchedulerByTradeId.containsKey(tradeId)) {
            closeTradeTimeoutSchedulerByTradeId.get(tradeId).stop();
        }
    }


    /* --------------------------------------------------------------------- */
    // Misc
    /* --------------------------------------------------------------------- */

    public Optional<MuSigOpenTradeChannel> findMuSigOpenTradeChannel(String tradeId) {
        return muSigOpenTradeChannelService.findChannelByTradeId(tradeId);
    }

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

    public ObservableHashMap<String, MuSigTrade> getTradeById() {
        return persistableStore.getTradeById();
    }

    public MusigGrpc.MusigBlockingStub getMusigBlockingStub() {
        return musigGrpcClient.getBlockingStub();
    }

    public MusigGrpc.MusigStub getMusigAsyncStub() {
        return musigGrpcClient.getAsyncStub();
    }

    // The maker has added the salted account id to the AccountOptions.
    // We will use the payment method chosen by the taker to determine which account we had assigned to that offer.
    public Optional<Account<? extends PaymentMethod<?>, ?>> findMyAccount(MuSigTrade trade) {
        MuSigContract contract = trade.getContract();
        MuSigOffer offer = contract.getOffer();
        PaymentMethod<?> selectedPaymentMethod = contract.getQuoteSidePaymentMethodSpec().getPaymentMethod();
        Set<Account<? extends PaymentMethod<?>, ?>> matchingAccounts = serviceProvider.getAccountService().getAccounts(selectedPaymentMethod);
        Set<AccountOption> accountOptions = OfferOptionUtil.findAccountOptions(offer.getOfferOptions());
        return accountOptions.stream()
                .filter(accountOption -> accountOption.getPaymentMethod().equals(selectedPaymentMethod))
                .map(AccountOption::getSaltedAccountId)
                .flatMap(saltedAccountId -> OfferOptionUtil.findAccountFromSaltedAccountId(matchingAccounts, saltedAccountId, offer.getId()).stream())
                .findAny();
    }

    /* --------------------------------------------------------------------- */
    // TradeProtocol factory
    /* --------------------------------------------------------------------- */

    private MuSigProtocol makerCreatesProtocol(MuSigContract contract, NetworkId sender, NetworkId receiver) {
        // We only create the data required for the protocol creation.
        // Verification will happen in the MuSigTakeOfferRequestHandler
        MuSigOffer offer = contract.getOffer();
        boolean isBuyer = offer.getMakersDirection().isBuy();
        Identity myIdentity = identityService.findAnyIdentityByNetworkId(offer.getMakerNetworkId()).orElseThrow();
        MuSigTrade trade = new MuSigTrade(contract, isBuyer, false, myIdentity, offer, sender, receiver);

        AccountPayload<? extends PaymentMethod<?>> accountPayload = findMyAccount(trade).orElseThrow().getAccountPayload();
        trade.getMyself().setAccountPayload(accountPayload);

        String tradeId = trade.getId();
        checkArgument(findProtocol(tradeId).isEmpty(), "We received the MuSigTakeOfferRequest for an already existing protocol");
        checkArgument(!tradeExists(tradeId), "A trade with that ID exists already");
        persistableStore.addTrade(trade);
        persist();

        maybeAddPeerToContactList(sender.getId());

        return createAndAddTradeProtocol(trade);
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
                            .orElseGet(() -> trade.getContract().getTakeOfferDate() < redactDateForNotCompletedTrades);
                    //todo
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

    private void maybeAddPeerToContactList(String peersProfileId) {
        if (settingsService.getDoAutoAddToContactList()) {
            userProfileService.findUserProfile(peersProfileId)
                    .ifPresent(userProfile -> contactListService.addContactListEntry(userProfile, ContactReason.MUSIG_TRADE));
        }
    }
}