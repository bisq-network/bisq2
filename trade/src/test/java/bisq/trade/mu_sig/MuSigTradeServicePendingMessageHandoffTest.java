/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.mu_sig;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.fsm.Event;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.OfferService;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.settings.SettingsService;
import bisq.trade.ServiceProvider;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import bisq.trade.mu_sig.messages.network.PaymentInitiatedMessage_E;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.messages.network.handler.maker.MuSigTakeOfferRequestValidator;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
import bisq.trade.protobuf.MusigGrpc;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Rule 1 of docs/specifications/trade/pending-trade-messages.md for MuSig: a trade message which the node receives
 * while it creates the protocol of its trade reaches the protocol.
 * <p>
 * On the maker's node, the take offer request creates the protocol, and the protocol then handles the request and
 * processes the pending messages. Handling a real request needs the MuSig daemon, so most of these tests create the
 * protocol of a stored trade with {@code initialize()} and then pass a first message of the peer to the protocol. This
 * is the same handoff: the creation of the protocol, followed by the first message which the protocol handles. One test
 * passes a take offer request through the path of the service. The handler of the request fails in that test, because
 * the test provides neither the services for its verification nor the daemon. The node must still pass the pending
 * messages to the protocol after the request.
 */
class MuSigTradeServicePendingMessageHandoffTest {
    private static final String MESSAGE_THREAD = "peer-message";
    private static final String PROTOCOL_THREAD = "protocol-creation";
    private static final Market MARKET = new Market("BTC", "EUR", "Bitcoin", "Euro");
    private static final FiatPaymentMethod PAYMENT_METHOD =
            FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);

    private Server server;
    private ServiceProvider serviceProvider;
    private UserService userService;
    private IdentityService identityService;
    private NetworkId peerNetworkId;
    private NetworkId myNetworkId;
    private Identity identity;
    private MuSigTrade trade;
    private MuSigTradeService service;

    @BeforeEach
    void setUp() throws Exception {
        server = Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create())
                .addService(new MusigGrpc.MusigImplBase() {
                })
                .build()
                .start();

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getConfidentialMessageServices()).thenReturn(Set.of());
        when(networkService.confidentialSend(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new SendMessageResult()));

        MuSigOpenTradeChannelService channelService = mock(MuSigOpenTradeChannelService.class);
        when(channelService.getChannels()).thenReturn(new ObservableSet<>());
        when(channelService.findChannelByTradeId(anyString())).thenReturn(Optional.empty());
        ChatService chatService = mock(ChatService.class);
        when(chatService.getMuSigOpenTradeChannelService()).thenReturn(channelService);

        userService = mock(UserService.class);
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));

        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        AlertService alertService = mock(AlertService.class);
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(new ObservableSet<>());
        when(bondedRolesService.getAlertService()).thenReturn(alertService);

        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getNumDaysAfterRedactingTradeData()).thenReturn(new Observable<>(90));

        @SuppressWarnings("unchecked")
        Persistence<MuSigTradeStore> persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        PersistenceService persistenceService = mock(PersistenceService.class);
        when(persistenceService.getOrCreatePersistence(any(), any(), any(MuSigTradeStore.class)))
                .thenReturn(persistence);

        serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getNetworkService()).thenReturn(networkService);
        when(serviceProvider.getChatService()).thenReturn(chatService);
        when(serviceProvider.getUserService()).thenReturn(userService);
        when(serviceProvider.getBondedRolesService()).thenReturn(bondedRolesService);
        when(serviceProvider.getSettingsService()).thenReturn(settingsService);
        when(serviceProvider.getPersistenceService()).thenReturn(persistenceService);
        // The service reads it in its constructor
        identityService = mock(IdentityService.class);
        when(serviceProvider.getIdentityService()).thenReturn(identityService);

        myNetworkId = createNetworkId("my-key", 9997);
        peerNetworkId = createNetworkId("peer-key", 9998);
        identity = new Identity("my-identity", myNetworkId,
                new KeyBundle("my-key-bundle", KeyGeneration.generateDefaultEcKeyPair(),
                        TorKeyGeneration.generateKeyPair(),
                        I2PKeyGeneration.generateKeyPair()));
        trade = createSellerAsMakerTradeAtInit();
        service = spy(new MuSigTradeService(new MuSigTradeService.Config("127.0.0.1", server.getPort()),
                serviceProvider, AppType.DESKTOP));
        when(serviceProvider.getMuSigTradeService()).thenReturn(service);
        service.getPersistableStore().addTrade(trade);
    }

    @AfterEach
    void tearDown() throws Exception {
        service.shutdown().get(5, SECONDS);
        server.shutdownNow();
        server.awaitTermination(5, SECONDS);
    }

    // Before the fix, the error report stayed pending, and the trade did not learn that the peer failed it
    @Test
    void errorReportWhichFindsNoProtocolWhileTheProtocolIsCreatedReachesTheProtocol() throws Exception {
        MuSigReportErrorMessage report = reportErrorMessage();
        CountDownLatch lookupFoundNoProtocol = new CountDownLatch(1);
        CountDownLatch continueMessageThread = new CountDownLatch(1);
        AtomicBoolean paused = new AtomicBoolean();
        doAnswer(invocation -> {
            Object protocol = invocation.callRealMethod();
            if (MESSAGE_THREAD.equals(Thread.currentThread().getName()) &&
                    ((Optional<?>) protocol).isEmpty() &&
                    paused.compareAndSet(false, true)) {
                lookupFoundNoProtocol.countDown();
                assertThat(continueMessageThread.await(10, SECONDS)).isTrue();
            }
            return protocol;
        }).when(service).findProtocol(trade.getId());

        // The message thread finds no protocol and stops right after the lookup. The message is not pending yet.
        TestThread messageThread = new TestThread(MESSAGE_THREAD, () -> service.onMessage(report));
        assertThat(lookupFoundNoProtocol.await(10, SECONDS)).isTrue();
        assertThat(service.getPendingTradeMessages().isEmpty()).isTrue();

        // The other thread creates the protocol, which then handles a first message and processes the pending messages.
        // The thread finishes only when the executor has no task left, so without the fix the error report becomes
        // pending only after the task of the first message has looked at the pending messages.
        TestThread protocolThread = new TestThread(PROTOCOL_THREAD, () -> {
            service.initialize().join();
            service.onMessage(paymentInitiatedMessage("first-message"));
            awaitIdleExecutor();
        });
        awaitFinishedOrWaitingFor(protocolThread.thread, messageThread.thread);
        continueMessageThread.countDown();

        messageThread.join();
        protocolThread.join();

        assertErrorReportReachesTheProtocol();
    }

    // The order without overlap: the error report is pending when the protocol is created
    @Test
    void errorReportWhichArrivesBeforeTheProtocolExistsReachesTheProtocolAfterItsFirstMessage() throws Exception {
        service.onMessage(reportErrorMessage());
        assertThat(service.getPendingTradeMessages().getMessagesInArrivalOrder()).containsExactly(reportErrorMessage());
        service.initialize().get(5, SECONDS);

        service.onMessage(paymentInitiatedMessage("first-message"));

        assertErrorReportReachesTheProtocol();
    }

    // Each pending message reaches the protocol once. Two messages which the peer sent before the protocol existed are
    // pending. The first message which the protocol handles makes the node pass them to the protocol.
    @Test
    void eachPendingMessageOfTheTradeReachesTheProtocolOnce() throws Exception {
        PaymentInitiatedMessage_E firstPendingMessage = paymentInitiatedMessage("first-pending-message");
        PaymentInitiatedMessage_E secondPendingMessage = paymentInitiatedMessage("second-pending-message");
        service.onMessage(firstPendingMessage);
        service.onMessage(secondPendingMessage);
        service.initialize().get(5, SECONDS);
        awaitIdleExecutor();
        MuSigProtocol protocol = spy(service.findProtocol(trade.getId()).orElseThrow());
        service.getTradeProtocolById().put(trade.getId(), protocol);

        service.onMessage(paymentInitiatedMessage("first-message"));
        awaitIdleExecutor();

        verify(protocol).handle(firstPendingMessage);
        verify(protocol).handle(secondPendingMessage);
        assertThat(service.getPendingTradeMessages().isEmpty()).isTrue();
    }

    // Rule 1 of docs/specifications/trade/trade-message-sender.md: the node checks the sender of a pending message when
    // it passes the message to the protocol. Without the check, anybody who knows the trade ID could end the trade with
    // a report which became pending before the protocol existed.
    @Test
    void pendingMessageOfAThirdPartyIsDroppedWhenItIsPassedToTheProtocol() throws Exception {
        service.onMessage(reportErrorMessage(createNetworkId("third-party-key", 9996)));
        service.initialize().get(5, SECONDS);

        service.onMessage(paymentInitiatedMessage("first-message"));
        awaitIdleExecutor();

        assertThat(trade.getTradeState()).isEqualTo(MuSigTradeState.INIT);
        assertThat(service.getPendingTradeMessages().isEmpty())
                .as("the node took the pending message of the third party and dropped it")
                .isTrue();
    }

    // Rule 1 of docs/specifications/trade/pending-trade-messages.md: the node passes a pending message to the protocol
    // after the message which the protocol handled, in the task of that message. A MuSig handler can block on a call
    // of the MuSig daemon, so the protocol must not run on the thread which delivers the message.
    @Test
    void pendingMessageReachesTheProtocolAfterTheHandledMessageInItsTask() throws Exception {
        PaymentInitiatedMessage_E pendingMessage = paymentInitiatedMessage("pending-message");
        service.onMessage(pendingMessage);
        service.initialize().get(5, SECONDS);
        awaitIdleExecutor();
        MuSigProtocol protocol = spy(service.findProtocol(trade.getId()).orElseThrow());
        service.getTradeProtocolById().put(trade.getId(), protocol);
        Map<Event, Thread> threadByEvent = new ConcurrentHashMap<>();
        doAnswer(invocation -> {
            threadByEvent.put(invocation.getArgument(0), Thread.currentThread());
            return invocation.callRealMethod();
        }).when(protocol).handle(any());
        PaymentInitiatedMessage_E handledMessage = paymentInitiatedMessage("handled-message");

        service.onMessage(handledMessage);
        awaitIdleExecutor();

        InOrder inOrder = inOrder(protocol);
        inOrder.verify(protocol).handle(handledMessage);
        inOrder.verify(protocol).handle(pendingMessage);
        assertThat(threadByEvent.get(pendingMessage))
                .as("the pending message reaches the protocol in the task of the handled message")
                .isSameAs(threadByEvent.get(handledMessage))
                .isNotSameAs(Thread.currentThread());
    }

    // Rule 1 of docs/specifications/trade/pending-trade-messages.md: on the maker's node, the take offer request
    // registers the protocol, and the node passes the pending messages of the trade to the protocol right after the
    // request. This is the only path which registers a protocol while messages of its trade can be pending. The
    // validation of the request has its own tests, so the test replaces it. The maker finds its identity, its user
    // identity and the account of the offer, as a real maker does.
    @Test
    void takeOfferRequestPassesThePendingMessagesOfItsTradeToTheProtocol() throws Exception {
        service.initialize().get(5, SECONDS);
        String accountId = "my-account";
        String offerId = "take-offer-request-test-offer";
        AccountOption accountOption = new AccountOption(PAYMENT_METHOD,
                OfferOptionUtil.createdSaltedAccountId(accountId, offerId),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                new byte[20]);
        MuSigOffer offer = createOffer(offerId, List.of(new CollateralOption(0.25, 0.25), accountOption));
        // Another take offer date than the stored trade, so the request creates a new trade
        MuSigTrade requestedTrade = createSellerAsMakerTrade(offer, trade.getContract().getTakeOfferDate() + 1000);
        String tradeId = requestedTrade.getId();
        stubTheMakerOfTheOffer(accountId);
        SetupTradeMessage_A request = mock(SetupTradeMessage_A.class);
        when(request.getTradeId()).thenReturn(tradeId);
        when(request.getSender()).thenReturn(peerNetworkId);
        when(request.getReceiver()).thenReturn(myNetworkId);
        when(request.getContract()).thenReturn(requestedTrade.getContract());
        // A message which the peer sent after the request arrives first
        PaymentInitiatedMessage_E pendingMessage = paymentInitiatedMessage("pending-message", tradeId);
        service.onMessage(pendingMessage);
        assertThat(service.getPendingTradeMessages().getMessagesInArrivalOrder()).containsExactly(pendingMessage);

        // The service validates the request in the thread which delivers it. A static mock applies only in the thread
        // which created it.
        try (MockedStatic<MuSigTakeOfferRequestValidator> validator =
                     mockStatic(MuSigTakeOfferRequestValidator.class)) {
            validator.when(() -> MuSigTakeOfferRequestValidator.validateOffer(any(), any())).thenReturn(offer);
            service.onMessage(request);
        }
        awaitIdleExecutor();

        assertThat(service.findProtocol(tradeId)).isPresent();
        assertThat(service.getPendingTradeMessages().isEmpty())
                .as("the node passed the pending message to the protocol after the take offer request")
                .isTrue();
    }

    // The executor of the service has one thread and a queue, so it runs the tasks one after the other, in the order of
    // their submission. While a task of the test runs, no other task runs, and so no task can submit a further task. So
    // when the queue is empty while the task of the test runs, every task which was submitted before has run. A task can
    // submit further tasks, so we repeat until the queue is empty. The counters of the executor cannot show this: they
    // do not count a task which a worker has taken from the queue but not yet started.
    private void awaitIdleExecutor() throws Exception {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) service.getExecutor();
        assertThat(executor.getPoolSize()).as("the executor runs the tasks on one thread").isLessThanOrEqualTo(1);
        long deadline = System.nanoTime() + SECONDS.toNanos(10);
        while (!executor.submit(() -> executor.getQueue().isEmpty()).get(10, SECONDS)) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("The tasks of the trade did not finish");
            }
        }
    }

    // The protocol handles the messages in tasks on the executor of the service, so we wait, with a deadline, for the
    // result in the trade and in the pending messages.
    private void assertErrorReportReachesTheProtocol() throws InterruptedException {
        long deadline = System.nanoTime() + SECONDS.toNanos(10);
        while ((trade.getTradeState() != MuSigTradeState.FAILED_AT_PEER || !service.getPendingTradeMessages().isEmpty())
                && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertThat(trade.getTradeState()).isEqualTo(MuSigTradeState.FAILED_AT_PEER);
        assertThat(service.getPendingTradeMessages().isEmpty()).isTrue();
    }

    // Returns when the thread has finished, or when it waits for a monitor or lock which the owner thread holds
    private static void awaitFinishedOrWaitingFor(Thread thread, Thread owner) throws InterruptedException {
        long deadline = System.nanoTime() + SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (!thread.isAlive()) {
                return;
            }
            ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(thread.threadId());
            if (info != null && info.getLockOwnerId() == owner.threadId()) {
                return;
            }
            Thread.sleep(5);
        }
        throw new AssertionError(thread.getName() + " neither finished nor waited for " + owner.getName());
    }

    private interface Task {
        void run() throws Exception;
    }

    // A started thread whose failure the test reports
    private static final class TestThread {
        private final Thread thread;
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        private TestThread(String name, Task task) {
            thread = new Thread(() -> {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    failure.set(throwable);
                }
            }, name);
            thread.start();
        }

        private void join() throws InterruptedException {
            thread.join(SECONDS.toMillis(10));
            assertThat(thread.isAlive()).as(thread.getName() + " has finished").isFalse();
            assertThat(failure.get()).as(thread.getName() + " has not failed").isNull();
        }
    }

    private MuSigReportErrorMessage reportErrorMessage() {
        return reportErrorMessage(peerNetworkId);
    }

    private MuSigReportErrorMessage reportErrorMessage(NetworkId sender) {
        return new MuSigReportErrorMessage("report-of-" + sender.getPubKey().getKeyId(),
                trade.getId(),
                MuSigProtocol.VERSION,
                sender,
                myNetworkId,
                "The peer reported a trade failure",
                "",
                TradeProtocolFailure.UNKNOWN);
    }

    // The protocol of the seller as maker accepts this message only after the deposit confirmation, so in the first
    // state it keeps the message for later and does not change the trade
    private PaymentInitiatedMessage_E paymentInitiatedMessage(String messageId) {
        return paymentInitiatedMessage(messageId, trade.getId());
    }

    private PaymentInitiatedMessage_E paymentInitiatedMessage(String messageId, String tradeId) {
        return new PaymentInitiatedMessage_E(messageId, tradeId, MuSigProtocol.VERSION, peerNetworkId, myNetworkId,
                new byte[32]);
    }

    // The maker of the offer: its identity, an active user identity, and the account which the offer names
    private void stubTheMakerOfTheOffer(String accountId) {
        when(identityService.findAnyIdentityByNetworkId(myNetworkId)).thenReturn(Optional.of(identity));
        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userIdentityService.findUserIdentity(identity.getId())).thenReturn(Optional.of(mock(UserIdentity.class)));
        Account<?, ?> account = mock(Account.class);
        when(account.getId()).thenReturn(accountId);
        doReturn(mock(AccountPayload.class)).when(account).getAccountPayload();
        AccountService accountService = mock(AccountService.class);
        doReturn(Set.of(account)).when(accountService).getAccounts(any(PaymentMethod.class));
        when(serviceProvider.getAccountService()).thenReturn(accountService);
        when(serviceProvider.getOfferService()).thenReturn(mock(OfferService.class, RETURNS_DEEP_STUBS));
    }

    private MuSigTrade createSellerAsMakerTradeAtInit() {
        MuSigOffer offer = createOffer("pending-message-handoff-test-offer", List.of(new CollateralOption(0.25, 0.25)));
        return createSellerAsMakerTrade(offer, System.currentTimeMillis());
    }

    private MuSigOffer createOffer(String offerId, List<OfferOption> offerOptions) {
        return new MuSigOffer(offerId, myNetworkId, Direction.SELL, MARKET, new BaseSideFixedAmountSpec(100_000),
                new MarketPriceSpec(), List.of(PAYMENT_METHOD), offerOptions, MuSigProtocol.VERSION);
    }

    // The trade which the maker creates from a take offer request of the peer with this offer and take offer date
    private MuSigTrade createSellerAsMakerTrade(MuSigOffer offer, long takeOfferDate) {
        MuSigContract contract = new MuSigContract(takeOfferDate, offer, peerNetworkId,
                100_000, 3_500_000,
                PaymentMethodSpecUtil.createPaymentMethodSpec(PAYMENT_METHOD, MARKET.getQuoteCurrencyCode()),
                new byte[20], Optional.empty(), Optional.empty(), new MarketPriceSpec(), 0);
        return new MuSigTrade(contract, false, false, identity, offer, peerNetworkId, myNetworkId);
    }

    private static NetworkId createNetworkId(String keyId, int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        return new NetworkId(new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port))),
                new PubKey(keyPair.getPublic(), keyId));
    }
}
