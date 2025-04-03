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

import bisq.account.payment_method.*;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.platform.Version;
import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.contract.Role;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.offer.Direction;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.settings.SettingsService;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import bisq.trade.TradeRole;
import bisq.trade.bisq_easy.protocol.*;
import bisq.trade.bisq_easy.protocol.events.*;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.chat.ChatChannelDomain;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public class BisqEasyTradeService implements PersistenceClient<BisqEasyTradeStore>, Service, ConfidentialMessageService.Listener {
    private final ServiceProvider serviceProvider;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final SettingsService settingsService;
    private final BannedUserService bannedUserService;
    private final AlertService alertService;
    private final UserIdentityService userIdentityService;

    private final Persistence<BisqEasyTradeStore> persistence;
    private final BisqEasyTradeStore persistableStore = new BisqEasyTradeStore();

    // We don't persist the protocol, only the model.
    private final Map<String, BisqEasyProtocol> tradeProtocolById = new ConcurrentHashMap<>();
    private boolean haltTrading;
    private boolean requireVersionForTrading;
    private Optional<String> minRequiredVersionForTrading = Optional.empty();

    private Pin authorizedAlertDataSetPin, numDaysAfterRedactingTradeDataPin;
    private Scheduler numDaysAfterRedactingTradeDataScheduler;
    private final Set<BisqEasyTradeMessage> pendingMessages = new CopyOnWriteArraySet<>();

    public BisqEasyTradeService(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        settingsService = serviceProvider.getSettingsService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        alertService = serviceProvider.getBondedRolesService().getAlertService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();

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

        networkService.removeConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // MessageListener
    /* --------------------------------------------------------------------- */

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
        BisqEasyProtocol protocol = createProtocol(bisqEasyContract, message.getSender(), message.getReceiver());
        protocol.handle(message);
        persist();

        if (!pendingMessages.isEmpty()) {
            log.info("We have pendingMessages. We try to re-process them now.");
            pendingMessages.forEach(this::handleBisqEasyTradeMessage);
        }
    }

    private void handleBisqEasyTradeMessage(BisqEasyTradeMessage message) {
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
                        pendingMessages.forEach(this::handleBisqEasyTradeMessage);
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

    public BisqEasyProtocol createBisqEasyProtocol(Identity takerIdentity,
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
        findProtocol(tradeId).ifPresentOrElse(protocol -> {
                    protocol.handle(event);
                    persist();
                },
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
        if (new Date().after(Trade.TRADE_ID_V1_ACTIVATION_DATE)) {
            // We do only check if we have a trade with same offer and takerNetworkId.
            // As we include after TRADE_ID_V1_ACTIVATION_DATE the takeOffer date in the trade ID we might have
            // multiple trades with the same offer and takerNetworkId combination.
            // To verify that we do not have the same trade we need to use the tradeExists method.
            return getTrades().stream().anyMatch(trade ->
                    trade.getOffer().getId().equals(bisqEasyOffer.getId()) &&
                            trade.getTaker().getNetworkId().getId().equals(takerNetworkId.getId())
            );
        } else {
            String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
            return tradeExists(tradeId);
        }
    }

    public ObservableSet<BisqEasyTrade> getTrades() {
        return persistableStore.getTrades();
    }

    public void removeTrade(BisqEasyTrade trade) {
        persistableStore.removeTrade(trade);
        tradeProtocolById.remove(trade.getId());
        persist();
    }


    /* --------------------------------------------------------------------- */
    // TradeProtocol factory
    /* --------------------------------------------------------------------- */

    private BisqEasyProtocol createProtocol(BisqEasyContract contract, NetworkId sender, NetworkId receiver) {
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
        return createAndAddTradeProtocol(bisqEasyTrade);
    }

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
                    if (StringUtils.isEmpty(trade.getPaymentAccountData().get())) {
                        return false;
                    }
                    boolean doRedaction = trade.getTradeCompletedDate().map(date -> date < redactDate)
                            .orElse(trade.getContract().getTakeOfferDate() < redactDateForNotCompletedTrades);
                    if (doRedaction) {
                        trade.getPaymentAccountData().set(Res.get("data.redacted"));
                    }
                    return doRedaction;
                })
                .count();
        if (numChanges > 0) {
            persist();
        }
    }

    private void addMockTrades() {
        try {
            // Create first mock trade - BUY 0.000232512 BTC (23,251,200 satoshis)
            createAndAddMockTrade(true, 2325120L, "BTC/USD");

            // Create second mock trade - SELL 0.00124564 BTC (124,564,000 satoshis)
            createAndAddMockTrade(false, 124564L, "BTC/EUR");
        } catch (Exception e) {
            log.error("Error creating mock trades: {}", e.getMessage(), e);
        }
    }

    private void createAndAddMockTrade(boolean isBuyer, long baseSideAmount, String marketCode) {
        // Generate unique IDs for this mock trade
        String tradeId = "mock-trade-" + System.currentTimeMillis() + "-" + Math.abs(baseSideAmount);

        // Get current user identity and create a peer profile
        UserProfile myUserProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
        UserProfile peerProfile = createMockPeerProfile();

        // Create mock contract
        BisqEasyContract contract = createMockContract(baseSideAmount, marketCode, isBuyer);

        // Create mock trade
        BisqEasyTrade mockTrade = createMockBisqEasyTrade(tradeId, isBuyer, contract);

        // Create mock channel
        BisqEasyOpenTradeChannel mockChannel = createMockChannel(tradeId, myUserProfile, peerProfile);

            // Add trade to service (will trigger handleTradeAdded)
            getTrades().add(mockTrade);

            log.info("Mock trade created and added to the service");
            // Note: The channel would need to be added to channelService which is outside the scope
            // of this class. Consider injecting that service if you need this functionality.
    }

    private UserProfile createMockPeerProfile() {
        // Get a random user profile that's not our own
        UserProfile myProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();

        return userIdentityService.getUserIdentities().stream()
                .map(UserIdentity::getUserProfile)
                .filter(profile -> !profile.equals(myProfile))
                .findFirst()
                .orElse(myProfile); // Fallback to using our own profile if no other is available

    }

    private BisqEasyTrade createMockBisqEasyTrade(String tradeId, boolean isBuyer, BisqEasyContract contract) {
        // Get the UserProfile of the current user
        UserProfile myUserProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();

        // Get the NetworkId from the UserProfile
        NetworkId makerNetworkId = myUserProfile.getNetworkId();

        // Rest of the method remains the same as in your original implementation
        List<BitcoinPaymentMethod> bitcoinPaymentMethods = Collections.singletonList(
                BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)
        );
        List<FiatPaymentMethod> fiatPaymentMethods = Collections.singletonList(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)
        );

        BisqEasyOffer offer = new BisqEasyOffer(
                makerNetworkId,
                isBuyer ? Direction.BUY : Direction.SELL,
                contract.getOffer().getMarket(),
                null,
                contract.getPriceSpec(),
                bitcoinPaymentMethods,
                fiatPaymentMethods,
                "Mock trade terms",
                Collections.singletonList("en")
        );

        // Get the Identity from the current selected UserIdentity
        Identity myIdentity = userIdentityService.getSelectedUserIdentity().getIdentity();

        return new BisqEasyTrade(
                contract,
                isBuyer,
                false,
                myIdentity,
                offer,
                makerNetworkId,
                makerNetworkId
        );
    }

    private BisqEasyOpenTradeChannel createMockChannel(String tradeId, UserProfile myUserProfile, UserProfile peerProfile) {
        return BisqEasyOpenTradeChannel.createByTrader(
                tradeId,
                createMockOffer(tradeId, myUserProfile, peerProfile),  // Create a mock offer
                userIdentityService.getSelectedUserIdentity(),  // Current user's identity
                peerProfile,  // Peer profile
                Optional.empty()  // No mediator
        );
    }

    private BisqEasyOffer createMockOffer(String tradeId, UserProfile myUserProfile, UserProfile peerProfile) {
        // Create a mock market
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");

        // Get the NetworkId from the UserProfile
        NetworkId makerNetworkId = myUserProfile.getNetworkId();

        // For mocks, we'll use these basic payment methods
        List<BitcoinPaymentMethod> bitcoinPaymentMethods = List.of(
                BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)
        );

        List<FiatPaymentMethod> fiatPaymentMethods = List.of(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)
        );

        // Using the constructor that takes payment methods directly
        return new BisqEasyOffer(
                makerNetworkId,
                Direction.BUY,  // Example direction
                market,
                null,  // AmountSpec can be null for mock
                new MarketPriceSpec(),
                bitcoinPaymentMethods,
                fiatPaymentMethods,
                "Mock trade terms",  // Maker's trade terms
                List.of("en")  // Supported language codes
        );
    }

    private BisqEasyContract createMockContract(long baseSideAmount, String marketCode, boolean isBuyer) {
        // Parse market code to get base and quote currencies
        String[] marketParts = marketCode.split("/");
        String baseCurrency = marketParts[0];
        String quoteCurrency = marketParts[1];

        // Create market with proper names for the currencies
        String baseCurrencyName = baseCurrency.equals("BTC") ? "Bitcoin" : baseCurrency;
        String quoteCurrencyName = quoteCurrency.equals("USD") ? "US Dollar" :
                quoteCurrency.equals("EUR") ? "Euro" : quoteCurrency;
        Market market = new Market(baseCurrency, quoteCurrency, baseCurrencyName, quoteCurrencyName);

        // Create mock price (e.g., BTC at $50,000)
        long price = 5000000L; // $50,000
        long quoteSideAmount = (baseSideAmount * price) / 100000000L; // Convert to quote currency

        // Create payment methods
        BitcoinPaymentRail paymentRail = BitcoinPaymentRail.MAIN_CHAIN;
        BitcoinPaymentMethod btcMethod = BitcoinPaymentMethod.fromPaymentRail(paymentRail);
        FiatPaymentMethod fiatMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);

        // Create price spec (market price)
        PriceSpec priceSpec = new MarketPriceSpec();

        // Create a unique offer ID
        String offerId = "mock-offer-" + System.currentTimeMillis() + "-" + Math.abs(baseSideAmount);

        // Get the NetworkId from the UserProfile
        NetworkId makerNetworkId = userIdentityService.getSelectedUserIdentity().getUserProfile().getNetworkId();

        // Create the BisqEasyOffer using the correct constructor
        BisqEasyOffer offer = new BisqEasyOffer(
                offerId,
                System.currentTimeMillis(),
                makerNetworkId,
                isBuyer ? Direction.BUY : Direction.SELL,
                market,
                null,  // AmountSpec can be null for mock
                priceSpec,
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(btcMethod, Optional.empty())),
                List.of(new FiatPaymentMethodSpec(fiatMethod, Optional.empty())),
                new ArrayList<>(),  // OfferOptions
                List.of("en")  // Supported language codes
        );

        // Get the taker's NetworkId - for mocks we'll use our own identity
        NetworkId takerNetworkId = userIdentityService.getSelectedUserIdentity().getUserProfile().getNetworkId();

        // Create the payment method specs
        BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec = new BitcoinPaymentMethodSpec(btcMethod, Optional.empty());
        FiatPaymentMethodSpec fiatPaymentMethodSpec = new FiatPaymentMethodSpec(fiatMethod, Optional.empty());

        // Create the contract
        return new BisqEasyContract(
                System.currentTimeMillis(), // takeOfferDate
                offer,
                takerNetworkId,
                baseSideAmount,
                quoteSideAmount,
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                Optional.empty(), // No mediator
                priceSpec,
                price // Current market price
        );
    }

    /**
     * Creates and adds completed mock trades for testing purposes
     * @return List of created mock trades
     */
    public List<BisqEasyTrade> createAndAddCompletedMockTrades() {
        List<BisqEasyTrade> mockTrades = new ArrayList<>();
        try {
            // Create first mock trade - BUY 0.000232512 BTC
            BisqEasyTrade buyTrade = createAndAddCompletedMockTrade(true, 2325120L, "BTC/USD");
            mockTrades.add(buyTrade);

            // Create second mock trade - SELL 0.00124564 BTC
            BisqEasyTrade sellTrade = createAndAddCompletedMockTrade(false, 124564L, "BTC/EUR");
            mockTrades.add(sellTrade);
        } catch (Exception e) {
            log.error("Error creating mock completed trades: {}", e.getMessage(), e);
        }
        return mockTrades;
    }

    private BisqEasyTrade createAndAddCompletedMockTrade(boolean isBuyer, long baseSideAmount, String marketCode) {
        // Generate unique IDs for this mock trade
        String tradeId = "mock-trade-" + System.currentTimeMillis() + "-" + Math.abs(baseSideAmount);

        // Get current user identity
        UserProfile myUserProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
        NetworkId myNetworkId = myUserProfile.getNetworkId();
        Identity myIdentity = userIdentityService.getSelectedUserIdentity().getIdentity();

        // Create mock contract
        BisqEasyContract contract = createMockContract(baseSideAmount, marketCode, isBuyer);

        // For completed mock trades, we'll set them as the maker role
        TradeRole tradeRole = isBuyer ? TradeRole.BUYER_AS_MAKER : TradeRole.SELLER_AS_MAKER;

        // Create taker and maker parties
        BisqEasyTradeParty taker = new BisqEasyTradeParty(myNetworkId);
        BisqEasyTradeParty maker = new BisqEasyTradeParty(myNetworkId);

        // Create the trade with BTC_CONFIRMED state
        BisqEasyTrade mockTrade = new BisqEasyTrade(
                contract,
                BisqEasyTradeState.BTC_CONFIRMED,
                tradeId,
                tradeRole,
                myIdentity,
                taker,
                maker
        );

        // Set completion date and other data
        mockTrade.setTradeCompletedDate(Optional.of(System.currentTimeMillis() - 86400000)); // 24 hours ago
        mockTrade.getPaymentAccountData().set("mock-account-data for " + marketCode);
        mockTrade.getBitcoinPaymentData().set("mock-btc-payment-data-" + tradeId);
        mockTrade.getPaymentProof().set("mock-transaction-id-" + tradeId);

        // Add trade to service
        persistableStore.addTrade(mockTrade);
        createAndAddTradeProtocol(mockTrade);

        log.info("Created and added completed mock trade: {}", tradeId);
        return mockTrade;
    }
}