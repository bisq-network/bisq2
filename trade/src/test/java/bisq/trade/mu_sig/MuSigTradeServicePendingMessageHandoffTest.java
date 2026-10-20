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

import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.CollateralOption;
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
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
import bisq.trade.protobuf.MusigGrpc;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Rule 1 of docs/specifications/trade/pending-trade-messages.md for MuSig: a trade message which the node receives
 * while it creates the protocol of its trade reaches the protocol.
 * <p>
 * On the maker's node, the take offer request creates the protocol, and the protocol then handles the request and
 * processes the pending messages. A test of the real request needs the MuSig daemon, so these tests create the
 * protocol of a stored trade with {@code initialize()} and then pass a first message of the peer to the protocol. This
 * is the same handoff: the creation of the protocol, followed by the first message which the protocol handles.
 */
class MuSigTradeServicePendingMessageHandoffTest {
    private static final String MESSAGE_THREAD = "peer-message";
    private static final String PROTOCOL_THREAD = "protocol-creation";

    private Server server;
    private ServiceProvider serviceProvider;
    private NetworkId peerNetworkId;
    private NetworkId myNetworkId;
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

        UserService userService = mock(UserService.class);
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

        myNetworkId = createNetworkId("my-key", 9997);
        peerNetworkId = createNetworkId("peer-key", 9998);
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
        // The thread finishes only after the task of the first message, so without the fix the error report becomes
        // pending only after that task has looked at the pending messages.
        TestThread protocolThread = new TestThread(PROTOCOL_THREAD, () -> {
            service.initialize().join();
            service.onMessage(paymentInitiatedMessage("first-message"));
            awaitTasksSubmittedSoFar();
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

    // The executor of the service has one thread and a queue, so it runs the tasks in the order of their submission. A
    // task which we submit now runs after every task which was submitted before it. The counters of the executor cannot
    // show this: they do not count a task which a worker has taken from the queue but not yet started.
    private void awaitTasksSubmittedSoFar() throws Exception {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) service.getExecutor();
        assertThat(executor.getPoolSize()).as("the executor runs the tasks on one thread").isLessThanOrEqualTo(1);
        executor.submit(() -> {
        }).get(10, SECONDS);
    }

    // The task of a message which the protocol handles submits a task for each pending message, so the error report
    // reaches the protocol in a later task. We wait for its result instead of waiting for the tasks.
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
        return new MuSigReportErrorMessage("report-of-peer",
                trade.getId(),
                MuSigProtocol.VERSION,
                peerNetworkId,
                myNetworkId,
                "The peer reported a trade failure",
                "",
                TradeProtocolFailure.UNKNOWN);
    }

    // The protocol of the seller as maker accepts this message only after the deposit confirmation, so in the first
    // state it keeps the message for later and does not change the trade
    private PaymentInitiatedMessage_E paymentInitiatedMessage(String messageId) {
        return new PaymentInitiatedMessage_E(messageId, trade.getId(), MuSigProtocol.VERSION, peerNetworkId, myNetworkId,
                new byte[32]);
    }

    private MuSigTrade createSellerAsMakerTradeAtInit() {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Identity identity = new Identity("my-identity", myNetworkId,
                new KeyBundle("my-key-bundle", keyPair,
                        TorKeyGeneration.generateKeyPair(),
                        I2PKeyGeneration.generateKeyPair()));
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        FiatPaymentMethod paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        MarketPriceSpec priceSpec = new MarketPriceSpec();
        MuSigOffer offer = new MuSigOffer("pending-message-handoff-test-offer", myNetworkId,
                Direction.SELL, market, new BaseSideFixedAmountSpec(100_000), priceSpec,
                List.of(paymentMethod), List.of(new CollateralOption(0.25, 0.25)), MuSigProtocol.VERSION);
        MuSigContract contract = new MuSigContract(System.currentTimeMillis(), offer, peerNetworkId,
                100_000, 3_500_000,
                PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, market.getQuoteCurrencyCode()),
                new byte[20], Optional.empty(), Optional.empty(), priceSpec, 0);
        return new MuSigTrade(contract, false, false, identity, offer, peerNetworkId, myNetworkId);
    }

    private static NetworkId createNetworkId(String keyId, int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        return new NetworkId(new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port))),
                new PubKey(keyPair.getPublic(), keyId));
    }
}
