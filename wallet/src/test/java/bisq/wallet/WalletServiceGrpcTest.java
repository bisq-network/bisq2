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

package bisq.wallet;

import bisq.common.monetary.Coin;
import bisq.persistence.PersistenceService;
import bisq.wallet.protobuf.GetBalanceRequest;
import bisq.wallet.protobuf.GetBalanceResponse;
import bisq.wallet.protobuf.GetSeedWordsRequest;
import bisq.wallet.protobuf.GetSeedWordsResponse;
import bisq.wallet.protobuf.GetUnusedAddressRequest;
import bisq.wallet.protobuf.GetUnusedAddressResponse;
import bisq.wallet.protobuf.IsWalletReadyRequest;
import bisq.wallet.protobuf.IsWalletReadyResponse;
import bisq.wallet.protobuf.GetWalletAddressesRequest;
import bisq.wallet.protobuf.GetWalletAddressesResponse;
import bisq.wallet.protobuf.ListTransactionsRequest;
import bisq.wallet.protobuf.ListTransactionsResponse;
import bisq.wallet.protobuf.ListUtxosRequest;
import bisq.wallet.protobuf.ListUtxosResponse;
import bisq.wallet.protobuf.OpenOrCreateWalletRequest;
import bisq.wallet.protobuf.OpenOrCreateWalletResponse;
import bisq.wallet.protobuf.PubAddressInfo;
import bisq.wallet.protobuf.SendToAddressRequest;
import bisq.wallet.protobuf.SendToAddressResponse;
import bisq.wallet.protobuf.TransactionOutput;
import bisq.wallet.protobuf.Utxo;
import bisq.wallet.protobuf.WalletGrpc;
import bisq.wallet.receive_address.ReceiveAddressEntry;
import bisq.wallet.vo.BalanceBreakdown;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The real WalletService against an in-process daemon stand-in: what goes into the requests, what comes out of
// the responses.
class WalletServiceGrpcTest {
    @TempDir
    private Path tempDirPath;

    private final List<Object> requests = Collections.synchronizedList(new ArrayList<>());
    private final List<String> unusedAddresses = new ArrayList<>();
    private final RecordingWallet daemon = new RecordingWallet();
    private Server server;
    private ManagedChannel channel;
    private WalletGrpcClient client;
    private PersistenceService persistenceService;
    private WalletService walletService;
    private WalletService pollingService;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(daemon)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        client = new WalletGrpcClient(channel);
        client.initialize();
        persistenceService = new PersistenceService(tempDirPath);
        walletService = new WalletService(new WalletService.Config(true, "localhost", 0, false), persistenceService, client);
    }

    // The store writes asynchronously and JUnit fails when a write still creates files in the temp dir after the test.
    // A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        if (pollingService != null) {
            pollingService.shutdown().join();
        }
        persistenceService.persistAllClients().join();
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void openOrCreateWalletSendsThePassword() throws ExecutionException, InterruptedException {
        walletService.openOrCreateWallet("Wallet-Pa55!").get();

        assertEquals("Wallet-Pa55!", ((OpenOrCreateWalletRequest) requests.get(0)).getPassword());
    }

    @Test
    void getSeedWordsSendsThePassword() throws ExecutionException, InterruptedException {
        List<String> seedWords = walletService.getSeedWords("Wallet-Pa55!").get();

        assertEquals("Wallet-Pa55!", ((GetSeedWordsRequest) requests.get(0)).getPassword());
        assertEquals(List.of("word1", "word2"), seedWords);
    }

    @Test
    void sendToAddressWrapsTheAddressAndSendsThePassphrase() throws ExecutionException, InterruptedException {
        String txId = walletService.sendToAddress(Optional.of("Wallet-Pa55!"), "bcrt1qreceiver", 1234L).get();

        SendToAddressRequest request = (SendToAddressRequest) requests.get(0);
        assertEquals("Wallet-Pa55!", request.getPassphrase());
        assertEquals("bcrt1qreceiver", request.getAddress().getAddress());
        assertEquals(1234L, request.getAmount());
        assertEquals("sent_tx_id", txId);
    }

    @Test
    void aFeeRateGoesOutOnlyWhenGiven() throws ExecutionException, InterruptedException {
        walletService.sendToAddress(Optional.of("Wallet-Pa55!"), "bcrt1qreceiver", 1234L, OptionalLong.of(750L)).get();
        walletService.sendToAddress(Optional.of("Wallet-Pa55!"), "bcrt1qreceiver", 1234L).get();

        List<SendToAddressRequest> sends = requests.stream()
                .filter(SendToAddressRequest.class::isInstance)
                .map(SendToAddressRequest.class::cast)
                .toList();
        SendToAddressRequest withFeeRate = sends.get(0);
        SendToAddressRequest withoutFeeRate = sends.get(1);
        assertTrue(withFeeRate.hasFeeRatePerKwu());
        assertEquals(750L, withFeeRate.getFeeRatePerKwu());
        assertFalse(withoutFeeRate.hasFeeRatePerKwu());
    }

    @Test
    void requestBalanceKeepsTheSpendableTotalAndPublishesTheBreakdown() throws ExecutionException, InterruptedException {
        Coin total = walletService.requestBalance().get();

        assertEquals(2000L, total.getValue());
        assertEquals(Coin.asBtcFromValue(2000), walletService.getBalance().get());
        assertEquals(new BalanceBreakdown(2000, 1500, 500, 7, 3), walletService.getBalanceBreakdown().get());
    }

    @Test
    void anAbsentPassphraseIsSentEmpty() throws ExecutionException, InterruptedException {
        walletService.sendToAddress(Optional.empty(), "bcrt1qreceiver", 1234L).get();

        assertEquals("", ((SendToAddressRequest) requests.get(0)).getPassphrase());
    }

    @Test
    void getUnusedAddressStoresTheAddressOnce() throws ExecutionException, InterruptedException {
        unusedAddresses.add("bcrt1qfirst");
        unusedAddresses.add("bcrt1qfirst");

        ReceiveAddressEntry first = walletService.getUnusedAddress().get();
        ReceiveAddressEntry again = walletService.getUnusedAddress().get();

        assertEquals("bcrt1qfirst", first.getAddress());
        assertSame(first, again);
        assertEquals(Optional.of(first), walletService.findReceiveAddressEntry("bcrt1qfirst"));
    }

    @Test
    void createReceiveAddressKeepsTheNoteOfAnAddressTheDaemonHandsOutAgain() throws ExecutionException, InterruptedException {
        unusedAddresses.add("bcrt1qcycled");
        unusedAddresses.add("bcrt1qcycled");
        ReceiveAddressEntry first = walletService.createReceiveAddress().get();
        walletService.updateReceiveAddress(first, Optional.of("rent"));

        ReceiveAddressEntry again = walletService.createReceiveAddress().get();

        assertEquals("bcrt1qcycled", again.getAddress());
        assertEquals(Optional.of("rent"), again.getNote());
    }

    @Test
    void requestWalletAddressesUnwrapsTheAddresses() throws ExecutionException, InterruptedException {
        var addresses = walletService.requestWalletAddresses().get();

        assertTrue(addresses.contains("bcrt1qone"));
        assertTrue(addresses.contains("bcrt1qtwo"));
        assertEquals(2, addresses.size());
    }

    @Test
    void outputsAndUtxosWithoutAStandardAddressHaveAnEmptyAddress() throws ExecutionException, InterruptedException {
        List<bisq.wallet.vo.Transaction> transactions = walletService.listTransactions().get();
        List<bisq.wallet.vo.Utxo> utxos = walletService.listUtxos().get();

        assertEquals("bcrt1qpaid", transactions.get(0).getOutputs().get(0).getAddress());
        assertEquals("", transactions.get(0).getOutputs().get(1).getAddress());
        assertEquals("bcrt1qowned", utxos.get(0).getAddress());
        assertEquals("", utxos.get(1).getAddress());
    }

    @Test
    void aNewInstallIsNotSetUpWithoutAskingTheDaemon() throws ExecutionException, InterruptedException {
        walletService.initialize().get();
        walletService.refreshState().get();

        assertEquals(WalletService.State.NOT_SET_UP, walletService.getState().get());
        assertTrue(requests.isEmpty());
    }

    @Test
    void openingTheWalletRecordsItAndTheRecordSurvivesARestart() throws ExecutionException, InterruptedException {
        walletService.initialize().get();
        walletService.openOrCreateWallet("Wallet-Pa55!").get();
        assertEquals(WalletService.State.OPEN, walletService.getState().get());

        awaitStoredWalletRecord();
        PersistenceService restartedPersistenceService = new PersistenceService(tempDirPath);
        WalletService restarted = new WalletService(new WalletService.Config(true, "localhost", 0, false),
                restartedPersistenceService, client);
        restartedPersistenceService.readAllPersisted().join();
        requests.clear();

        restarted.initialize().get();
        restarted.refreshState().get();

        assertEquals(WalletService.State.OPEN, restarted.getState().get());
        assertTrue(requests.stream().anyMatch(GetBalanceRequest.class::isInstance));
    }

    @Test
    void openingTheWalletFetchesTheBalance() throws ExecutionException, InterruptedException {
        walletService.initialize().get();

        walletService.openOrCreateWallet("Wallet-Pa55!").get();

        awaitBalance();
        assertEquals(Coin.asBtcFromValue(2000), walletService.getBalance().get());
    }

    @Test
    void aClosedWalletOnTheDaemonIsLocked() throws ExecutionException, InterruptedException {
        openWallet();
        daemon.walletOpen = false;

        walletService.refreshState().get();

        assertEquals(WalletService.State.LOCKED, walletService.getState().get());
    }

    @Test
    void anOpenWalletThatIsStillSyncingIsOpenAndNeedsNoPassword() throws ExecutionException, InterruptedException {
        openWallet();
        daemon.ready = false;
        requests.clear();

        walletService.refreshState().get();

        assertEquals(WalletService.State.OPEN, walletService.getState().get());
        assertFalse(requests.stream().anyMatch(IsWalletReadyRequest.class::isInstance));
    }

    @Test
    void anUnreachableDaemonIsUnavailable() throws ExecutionException, InterruptedException {
        openWallet();
        daemon.failure = Status.UNAVAILABLE;

        walletService.refreshState().get();

        assertEquals(WalletService.State.UNAVAILABLE, walletService.getState().get());
    }

    @Test
    void aDaemonWithoutTheWalletServiceIsUnavailable() throws ExecutionException, InterruptedException {
        openWallet();
        daemon.failure = Status.UNIMPLEMENTED;

        walletService.refreshState().get();

        assertEquals(WalletService.State.UNAVAILABLE, walletService.getState().get());
    }

    @Test
    void anUnclassifiedProbeErrorOnAnInstallWithAWalletIsUnavailableNotSetup() throws ExecutionException, InterruptedException {
        openWallet();
        awaitStoredWalletRecord();
        PersistenceService restartedPersistenceService = new PersistenceService(tempDirPath);
        WalletService restarted = new WalletService(new WalletService.Config(true, "localhost", 0, false),
                restartedPersistenceService, client);
        restartedPersistenceService.readAllPersisted().join();
        daemon.failure = Status.INTERNAL;

        restarted.initialize().get();
        restarted.refreshState().get();

        assertEquals(WalletService.State.UNAVAILABLE, restarted.getState().get());
    }

    @Test
    void aWrongPasswordOnALockedWalletKeepsItLocked() throws ExecutionException, InterruptedException {
        openWallet();
        daemon.walletOpen = false;
        walletService.refreshState().get();
        assertEquals(WalletService.State.LOCKED, walletService.getState().get());

        assertThrows(ExecutionException.class, () -> walletService.openOrCreateWallet("wrong").get());

        assertEquals(WalletService.State.LOCKED, walletService.getState().get());
    }

    @Test
    void anOpenAttemptAgainstAnUnreachableDaemonBeforeSetupStaysNotSetUp() throws ExecutionException, InterruptedException {
        walletService.initialize().get();
        daemon.failure = Status.UNAVAILABLE;

        assertThrows(ExecutionException.class, () -> walletService.openOrCreateWallet("Wallet-Pa55!").get());

        awaitState(WalletService.State.NOT_SET_UP);
    }

    @Test
    void anOpenAttemptAgainstAnUnreachableDaemonOnALockedWalletIsUnavailable() throws ExecutionException, InterruptedException {
        openWallet();
        daemon.walletOpen = false;
        walletService.refreshState().get();
        assertEquals(WalletService.State.LOCKED, walletService.getState().get());
        daemon.failure = Status.UNAVAILABLE;

        assertThrows(ExecutionException.class, () -> walletService.openOrCreateWallet("Wallet-Pa55!").get());

        awaitState(WalletService.State.UNAVAILABLE);
    }

    @Test
    void aRefusedPasswordLeavesTheStateAndTheRecordUntouched() throws ExecutionException, InterruptedException {
        walletService.initialize().get();
        daemon.walletOpen = false;

        assertThrows(ExecutionException.class, () -> walletService.openOrCreateWallet("wrong").get());
        walletService.refreshState().get();

        assertEquals(WalletService.State.NOT_SET_UP, walletService.getState().get());
        assertFalse(requests.stream().anyMatch(GetBalanceRequest.class::isInstance));
    }

    @Test
    void aNotOpenFailureOfAnOrdinaryCallIsAnsweredWithAProbe() throws Exception {
        openWallet();
        daemon.walletOpen = false;

        assertThrows(ExecutionException.class, () -> walletService.requestTransactions().get());

        awaitState(WalletService.State.LOCKED);
    }

    @Test
    void aProbeStartedBeforeTheUnlockCannotLockTheWalletAgain() throws ExecutionException, InterruptedException {
        openWallet();
        daemon.walletOpen = false;
        daemon.holdBalanceReply = true;
        CompletableFuture<Void> probe = walletService.refreshState();
        daemon.holdBalanceReply = false;

        walletService.openOrCreateWallet("Wallet-Pa55!").get();
        assertEquals(WalletService.State.OPEN, walletService.getState().get());
        daemon.heldBalanceReply.onError(Status.FAILED_PRECONDITION.asRuntimeException());
        probe.get();

        assertEquals(WalletService.State.OPEN, walletService.getState().get());
    }

    @Test
    void aTimedOutProbeKeepsTheState() throws Exception {
        openWallet();
        awaitStoredWalletRecord();
        WalletGrpcClient shortDeadlineClient = new WalletGrpcClient(channel, Duration.ofMillis(200));
        shortDeadlineClient.initialize();
        PersistenceService restartedPersistenceService = new PersistenceService(tempDirPath);
        WalletService slowDaemonService = new WalletService(new WalletService.Config(true, "localhost", 0, false),
                restartedPersistenceService, shortDeadlineClient);
        restartedPersistenceService.readAllPersisted().join();
        slowDaemonService.refreshState().get();
        assertEquals(WalletService.State.OPEN, slowDaemonService.getState().get());
        daemon.holdBalanceReply = true;

        slowDaemonService.refreshState().get(5, TimeUnit.SECONDS);

        assertEquals(WalletService.State.OPEN, slowDaemonService.getState().get());
    }

    @Test
    void aFirstProbeWhichTimesOutOnAnInstallWithAWalletIsUnavailable() throws Exception {
        openWallet();
        awaitStoredWalletRecord();
        WalletGrpcClient shortDeadlineClient = new WalletGrpcClient(channel, Duration.ofMillis(200));
        shortDeadlineClient.initialize();
        PersistenceService restartedPersistenceService = new PersistenceService(tempDirPath);
        WalletService restarted = new WalletService(new WalletService.Config(true, "localhost", 0, false),
                restartedPersistenceService, shortDeadlineClient);
        restartedPersistenceService.readAllPersisted().join();
        daemon.holdBalanceReply = true;

        restarted.refreshState().get(5, TimeUnit.SECONDS);

        assertEquals(WalletService.State.UNAVAILABLE, restarted.getState().get());
    }

    @Test
    void theBalanceIsPolledWhileTheWalletIsOpenAndTransactionsFollowAChange() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        requests.clear();
        daemon.balance = 3000;

        awaitBalance(polling, 3000);
        awaitRequest(ListTransactionsRequest.class);
    }

    @Test
    void anIncomingPaymentThatIsStillPendingRefreshesTheTransactions() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        Thread.sleep(150);
        requests.clear();
        daemon.untrustedPending = 500;

        awaitRequest(ListTransactionsRequest.class);
    }

    @Test
    void anUnreachableDaemonIsProbedByThePollUntilItIsBack() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        daemon.failure = Status.UNAVAILABLE;
        awaitState(polling, WalletService.State.UNAVAILABLE);

        daemon.failure = null;
        daemon.walletOpen = false;

        awaitState(polling, WalletService.State.LOCKED);
    }

    @Test
    void aSlowDaemonStillUpdatesTheBalance() throws Exception {
        openWallet();
        daemon.holdBalanceReply = true;
        CompletableFuture<Coin> first = walletService.requestBalance();
        StreamObserver<GetBalanceResponse> firstReply = daemon.heldBalanceReply;
        CompletableFuture<Coin> second = walletService.requestBalance();
        StreamObserver<GetBalanceResponse> secondReply = daemon.heldBalanceReply;
        daemon.holdBalanceReply = false;

        firstReply.onNext(GetBalanceResponse.newBuilder().setBalance(3000).build());
        firstReply.onCompleted();
        first.get();
        assertEquals(Coin.asBtcFromValue(3000), walletService.getBalance().get());

        secondReply.onNext(GetBalanceResponse.newBuilder().setBalance(4000).build());
        secondReply.onCompleted();
        second.get();
        assertEquals(Coin.asBtcFromValue(4000), walletService.getBalance().get());
    }

    @Test
    void nothingIsPolledWhileTheWalletIsNotSetUp() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();

        Thread.sleep(300);

        assertFalse(requests.stream().anyMatch(GetBalanceRequest.class::isInstance));
    }

    @Test
    void aRestartedDaemonIsNoticedByThePollWithoutAUserAction() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);

        daemon.walletOpen = false;

        awaitState(polling, WalletService.State.LOCKED);
    }

    @Test
    void theDaemonIsNotPolledUntilPollingIsStarted() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        requests.clear();
        Thread.sleep(300);
        assertFalse(requests.stream().anyMatch(GetBalanceRequest.class::isInstance));
    }

    @Test
    void startingThePollTwiceProbesOnce() throws Exception {
        WalletService polling = pollingService(Duration.ofSeconds(30));
        polling.initialize().get();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        requests.clear();
        polling.startPolling();
        polling.startPolling();
        Thread.sleep(300);
        assertEquals(1, requests.stream().filter(GetBalanceRequest.class::isInstance).count());
    }

    @Test
    void thePollCanBeStartedAgainAfterAStop() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        polling.stopPolling();
        daemon.balance = 3000;
        polling.startPolling();
        awaitBalance(polling, 3000);
    }

    @Test
    void aPollAnswerWhichArrivesAfterTheStopTriggersNoFollowUp() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        daemon.holdBalanceReply = true;
        awaitHeldBalanceReply();
        polling.stopPolling();
        requests.clear();
        daemon.holdBalanceReply = false;
        daemon.heldBalanceReply.onNext(GetBalanceResponse.newBuilder().setBalance(3000).build());
        daemon.heldBalanceReply.onCompleted();
        Thread.sleep(300);
        assertTrue(requests.isEmpty());
    }

    @Test
    void aPollFailureWhichArrivesAfterTheStopTriggersNoProbe() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        daemon.holdBalanceReply = true;
        awaitHeldBalanceReply();
        polling.stopPolling();
        requests.clear();
        daemon.holdBalanceReply = false;
        daemon.heldBalanceReply.onError(Status.UNAVAILABLE.asRuntimeException());
        Thread.sleep(300);
        assertTrue(requests.isEmpty());
    }

    @Test
    void aPollAnswerOfAStoppedPollTriggersNoFollowUpAfterARestart() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        daemon.holdBalanceReply = true;
        awaitHeldBalanceReply();
        StreamObserver<GetBalanceResponse> replyOfTheStoppedPoll = daemon.heldBalanceReply;
        polling.stopPolling();
        daemon.holdBalanceReply = false;
        daemon.balance = 3000;
        daemon.untrustedPending = 500;
        polling.startPolling();
        awaitBalance(polling, 3000);
        requests.clear();

        replyOfTheStoppedPoll.onNext(GetBalanceResponse.newBuilder()
                .setBalance(3000)
                .setConfirmed(1500)
                .setTrustedPending(500)
                .setUntrustedPending(500)
                .setImmature(3)
                .build());
        replyOfTheStoppedPoll.onCompleted();

        awaitRequest(GetBalanceRequest.class);
        assertFalse(requests.stream().anyMatch(ListTransactionsRequest.class::isInstance));
    }

    @Test
    void shutdownEndsThePoll() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        polling.shutdown().join();
        // A poll that survived the shutdown would fail on the closed channel and turn the state UNAVAILABLE.
        Thread.sleep(300);
        assertEquals(WalletService.State.OPEN, polling.getState().get());
    }

    @Test
    void startingThePollProbesTheStateAtOnce() throws Exception {
        WalletService polling = pollingService(Duration.ofSeconds(30));
        polling.initialize().get();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        daemon.walletOpen = false;
        polling.startPolling();
        awaitState(polling, WalletService.State.LOCKED);
    }

    @Test
    void stoppingThePollEndsTheDaemonCalls() throws Exception {
        WalletService polling = pollingService(Duration.ofMillis(50));
        polling.initialize().get();
        polling.startPolling();
        polling.openOrCreateWallet("Wallet-Pa55!").get();
        awaitBalance(polling, 2000);
        polling.stopPolling();
        Thread.sleep(100);
        requests.clear();
        Thread.sleep(300);
        assertTrue(requests.isEmpty());
    }

    @Test
    void aSendRefreshesTheBalanceAndTheTransactions() throws Exception {
        openWallet();
        awaitBalance();
        requests.clear();

        walletService.sendToAddress(Optional.of("Wallet-Pa55!"), "bcrt1qsomeone", 1000).get();

        awaitRequest(GetBalanceRequest.class);
        awaitRequest(ListTransactionsRequest.class);
    }

    @Test
    void aLateBalanceAnswerDoesNotOverwriteANewerOne() throws Exception {
        openWallet();
        daemon.holdBalanceReply = true;
        CompletableFuture<Coin> slow = walletService.requestBalance();
        daemon.holdBalanceReply = false;
        daemon.balance = 5000;
        walletService.requestBalance().get();
        assertEquals(Coin.asBtcFromValue(5000), walletService.getBalance().get());

        daemon.heldBalanceReply.onNext(GetBalanceResponse.newBuilder().setBalance(2000).build());
        daemon.heldBalanceReply.onCompleted();
        slow.get();

        assertEquals(Coin.asBtcFromValue(5000), walletService.getBalance().get());
    }

    @Test
    void initializingTwiceKeepsTheWalletOpen() throws ExecutionException, InterruptedException {
        openWallet();

        walletService.initialize().get();
        walletService.initialize().get();
        walletService.refreshState().get();

        assertEquals(WalletService.State.OPEN, walletService.getState().get());
    }

    private void openWallet() throws ExecutionException, InterruptedException {
        walletService.initialize().get();
        walletService.openOrCreateWallet("Wallet-Pa55!").get();
    }

    // The record is written asynchronously by the persistence service; a restart reads it from disk.
    private void awaitStoredWalletRecord() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (!new WalletStoreService(new PersistenceService(tempDirPath)).readPersisted()
                .map(WalletStore::isWalletExists).orElse(false)
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }

    private WalletService pollingService(Duration interval) {
        pollingService = new WalletService(new WalletService.Config(true, "localhost", 0, false), persistenceService, client, interval);
        return pollingService;
    }

    private void awaitHeldBalanceReply() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (daemon.heldBalanceReply == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertNotNull(daemon.heldBalanceReply, "no balance request was held");
    }

    private void awaitBalance() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (walletService.getBalance().get() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }

    private void awaitBalance(WalletService service, long expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (!Coin.asBtcFromValue(expected).equals(service.getBalance().get()) && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(Coin.asBtcFromValue(expected), service.getBalance().get());
    }

    private void awaitRequest(Class<?> requestType) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (requests.stream().noneMatch(requestType::isInstance) && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(requests.stream().anyMatch(requestType::isInstance), requestType.getSimpleName() + " was not requested");
    }

    private void awaitState(WalletService.State expected) throws InterruptedException {
        awaitState(walletService, expected);
    }

    private void awaitState(WalletService service, WalletService.State expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (service.getState().get() != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(expected, service.getState().get());
    }

    // The daemon's rules in miniature: a closed wallet refuses everything but open and ready, open needs the password,
    // a configured failure fails every call.
    private class RecordingWallet extends WalletGrpc.WalletImplBase {
        private volatile boolean walletOpen = true;
        private volatile boolean ready = true;
        private volatile long balance = 2000;
        private volatile long untrustedPending = 7;
        private volatile Status failure;
        private volatile boolean holdBalanceReply;
        private volatile StreamObserver<GetBalanceResponse> heldBalanceReply;

        @Override
        public void openOrCreateWallet(OpenOrCreateWalletRequest request, StreamObserver<OpenOrCreateWalletResponse> responseObserver) {
            requests.add(request);
            if (failure != null) {
                responseObserver.onError(failure.asRuntimeException());
                return;
            }
            if (!walletOpen) {
                if (!request.getPassword().equals("Wallet-Pa55!")) {
                    responseObserver.onError(Status.PERMISSION_DENIED.asRuntimeException());
                    return;
                }
                walletOpen = true;
            }
            reply(responseObserver, OpenOrCreateWalletResponse.newBuilder().setSuccess(true).build());
        }

        @Override
        public void isWalletReady(IsWalletReadyRequest request, StreamObserver<IsWalletReadyResponse> responseObserver) {
            requests.add(request);
            reply(responseObserver, IsWalletReadyResponse.newBuilder().setReady(walletOpen && ready).build());
        }

        @Override
        public void getSeedWords(GetSeedWordsRequest request, StreamObserver<GetSeedWordsResponse> responseObserver) {
            requests.add(request);
            if (!canServe(responseObserver)) {
                return;
            }
            reply(responseObserver, GetSeedWordsResponse.newBuilder().addSeedWords("word1").addSeedWords("word2").build());
        }

        @Override
        public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
            requests.add(request);
            if (holdBalanceReply) {
                heldBalanceReply = responseObserver;
                return;
            }
            if (!canServe(responseObserver)) {
                return;
            }
            reply(responseObserver, GetBalanceResponse.newBuilder()
                    .setBalance(balance)
                    .setConfirmed(1500)
                    .setTrustedPending(500)
                    .setUntrustedPending(untrustedPending)
                    .setImmature(3)
                    .build());
        }

        @Override
        public void sendToAddress(SendToAddressRequest request, StreamObserver<SendToAddressResponse> responseObserver) {
            requests.add(request);
            if (!canServe(responseObserver)) {
                return;
            }
            reply(responseObserver, SendToAddressResponse.newBuilder().setTxId("sent_tx_id").build());
        }

        @Override
        public void getUnusedAddress(GetUnusedAddressRequest request, StreamObserver<GetUnusedAddressResponse> responseObserver) {
            requests.add(request);
            if (!canServe(responseObserver)) {
                return;
            }
            reply(responseObserver, GetUnusedAddressResponse.newBuilder()
                    .setAddress(PubAddressInfo.newBuilder().setAddress(unusedAddresses.remove(0)))
                    .build());
        }

        @Override
        public void getWalletAddresses(GetWalletAddressesRequest request, StreamObserver<GetWalletAddressesResponse> responseObserver) {
            if (!canServe(responseObserver)) {
                return;
            }
            reply(responseObserver, GetWalletAddressesResponse.newBuilder()
                    .addAddresses(PubAddressInfo.newBuilder().setAddress("bcrt1qone"))
                    .addAddresses(PubAddressInfo.newBuilder().setAddress("bcrt1qtwo"))
                    .build());
        }

        @Override
        public void listTransactions(ListTransactionsRequest request, StreamObserver<ListTransactionsResponse> responseObserver) {
            requests.add(request);
            if (!canServe(responseObserver)) {
                return;
            }
            reply(responseObserver, ListTransactionsResponse.newBuilder()
                    .addTransactions(bisq.wallet.protobuf.Transaction.newBuilder()
                            .setTxId("tx1")
                            .addOutputs(TransactionOutput.newBuilder().setValue(1L)
                                    .setAddress(PubAddressInfo.newBuilder().setAddress("bcrt1qpaid")))
                            .addOutputs(TransactionOutput.newBuilder().setValue(2L)))
                    .build());
        }

        @Override
        public void listUtxos(ListUtxosRequest request, StreamObserver<ListUtxosResponse> responseObserver) {
            if (!canServe(responseObserver)) {
                return;
            }
            reply(responseObserver, ListUtxosResponse.newBuilder()
                    .addUtxos(Utxo.newBuilder().setTxId("tx1").setAmount(1L)
                            .setAddress(PubAddressInfo.newBuilder().setAddress("bcrt1qowned")))
                    .addUtxos(Utxo.newBuilder().setTxId("tx2").setAmount(2L))
                    .build());
        }

        private boolean canServe(StreamObserver<?> responseObserver) {
            if (failure != null) {
                responseObserver.onError(failure.asRuntimeException());
                return false;
            }
            if (!walletOpen) {
                responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("wallet not open").asRuntimeException());
                return false;
            }
            return true;
        }

        private <T> void reply(StreamObserver<T> responseObserver, T response) {
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
