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

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.release.AppType;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.market.MarketRepository;
import bisq.common.observable.Observable;
import bisq.contract.ContractService;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.SecurityService;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyAccountDataMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyBtcAddressMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferResponse;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;
import bisq.user.reputation.ReputationScore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.trade.bisq_easy.BisqEasyTakeOfferRequestTestFixtures.makerAcceptsTakeOfferRequests;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.channel;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.userProfile;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Rule 1 of docs/specifications/trade/pending-trade-messages.md: a trade message which the maker receives while the
 * take offer request creates the protocol of its trade reaches the protocol. The network layer processes the messages
 * of a trade on several threads and does not deliver a message again, so the maker processes the request and the
 * taker's next message at the same time.
 * <p>
 * Two trade services exchange the messages which their protocols send. The maker processes the take offer request with
 * the real code path. The tests force each order of the steps of the two threads, so they do not depend on timing.
 * They do not depend on how the node prevents the loss, with one exception: the node may let the request thread wait
 * until the message thread has decided whether the message is pending. The tests detect this wait and then let the
 * message thread continue.
 */
class BisqEasyTradeServicePendingMessageHandoffTest {
    private static final String ACCOUNT_DATA = "SEPA account of the seller";
    private static final String BTC_ADDRESS = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq";
    private static final String MESSAGE_THREAD = "taker-message";
    private static final String REQUEST_THREAD = "take-offer-request";

    private final List<Node> nodes = new ArrayList<>();

    @AfterEach
    void tearDown() {
        nodes.forEach(node -> node.service.shutdown().join());
    }

    // Before the fix, the account data stayed pending: both traders waited for each other, and nothing changed it
    @Test
    void accountDataWhichFindsNoProtocolWhileTheRequestCreatesItReachesTheBuyer() throws Exception {
        SellerTakesBuyOffer trade = new SellerTakesBuyOffer();
        BisqEasyTakeOfferRequest request = trade.seller.takeSentMessage(BisqEasyTakeOfferRequest.class);
        BisqEasyAccountDataMessage accountData = trade.seller.takeSentMessage(BisqEasyAccountDataMessage.class);

        deliverWithLookupBeforeTheProtocolExists(trade.buyer, request, accountData);

        assertThat(trade.buyer.protocolRegistry.hasPendingMessages()).isFalse();
        assertThat(trade.buyersTrade().getPaymentAccountData()).contains(ACCOUNT_DATA);
        assertThat(trade.buyersTrade().getTradeState()).isEqualTo(
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA);
        // The buyer answered the request and sent nothing else, in particular no error report
        assertThat(trade.buyer.sentMessages).singleElement().isInstanceOf(BisqEasyTakeOfferResponse.class);
        tradeContinuesToTheFiatPayment(trade);
    }

    // Before the fix, the Bitcoin address stayed pending until the buyer's "payment sent" message arrived
    @Test
    void btcAddressWhichFindsNoProtocolWhileTheRequestCreatesItReachesTheSellerAtOnce() throws Exception {
        BuyerTakesSellOffer trade = new BuyerTakesSellOffer();
        BisqEasyTakeOfferRequest request = trade.buyer.takeSentMessage(BisqEasyTakeOfferRequest.class);
        BisqEasyBtcAddressMessage btcAddress = trade.buyer.takeSentMessage(BisqEasyBtcAddressMessage.class);

        deliverWithLookupBeforeTheProtocolExists(trade.seller, request, btcAddress);

        assertThat(trade.seller.protocolRegistry.hasPendingMessages()).isFalse();
        assertThat(trade.sellersTrade().getBitcoinPaymentData()).contains(BTC_ADDRESS);
        assertThat(trade.sellersTrade().getTradeState()).isEqualTo(
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS);
        assertThat(trade.seller.sentMessages).singleElement().isInstanceOf(BisqEasyTakeOfferResponse.class);
    }

    // The other order: the message finds the protocol, which has not handled the request yet. The protocol of the
    // buyer as maker has a path for account data which arrives before the request, so it applies the data at once.
    @Test
    void accountDataWhichFindsTheProtocolBeforeItHandledTheRequestReachesTheBuyer() throws Exception {
        SellerTakesBuyOffer trade = new SellerTakesBuyOffer();
        BisqEasyTakeOfferRequest request = trade.seller.takeSentMessage(BisqEasyTakeOfferRequest.class);
        BisqEasyAccountDataMessage accountData = trade.seller.takeSentMessage(BisqEasyAccountDataMessage.class);

        deliverWithLookupAfterTheProtocolExists(trade.buyer, request, accountData);

        assertThat(trade.buyer.protocolRegistry.hasPendingMessages()).isFalse();
        assertThat(trade.buyersTrade().getPaymentAccountData()).contains(ACCOUNT_DATA);
        assertThat(trade.buyersTrade().getTradeState()).isEqualTo(
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_);
        assertThat(trade.buyer.sentMessages).singleElement().isInstanceOf(BisqEasyTakeOfferResponse.class);
        tradeContinuesToTheFiatPayment(trade);
    }

    // The order without overlap: the account data is pending when the request arrives
    @Test
    void accountDataWhichArrivesBeforeTheRequestIsAppliedAfterTheRequest() {
        SellerTakesBuyOffer trade = new SellerTakesBuyOffer();
        BisqEasyTakeOfferRequest request = trade.seller.takeSentMessage(BisqEasyTakeOfferRequest.class);
        BisqEasyAccountDataMessage accountData = trade.seller.takeSentMessage(BisqEasyAccountDataMessage.class);

        trade.buyer.service.onMessage(accountData);
        assertThat(trade.buyer.protocolRegistry.getPendingMessagesInArrivalOrder()).containsExactly(accountData);
        trade.buyer.service.onMessage(request);

        assertThat(trade.buyer.protocolRegistry.hasPendingMessages()).isFalse();
        assertThat(trade.buyersTrade().getPaymentAccountData()).contains(ACCOUNT_DATA);
        assertThat(trade.buyersTrade().getTradeState()).isEqualTo(
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA);
        tradeContinuesToTheFiatPayment(trade);
    }

    private static void tradeContinuesToTheFiatPayment(SellerTakesBuyOffer trade) {
        trade.buyer.deliverSentMessagesTo(trade.seller);
        trade.buyer.service.buyerSendBitcoinPaymentData(trade.buyersTrade(), BTC_ADDRESS);
        trade.buyer.service.buyerConfirmFiatSent(trade.buyersTrade());
        trade.buyer.deliverSentMessagesTo(trade.seller);
        assertThat(trade.buyersTrade().getTradeState()).isEqualTo(BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION);
        assertThat(trade.sellersTrade.getTradeState()).isEqualTo(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION);
    }

    // The message thread looks for the protocol, finds none, and stops right after the lookup. The message is not
    // pending yet. Then the request thread creates the trade and its protocol. The message thread continues when the
    // request thread has processed the request completely, including the pending messages, or when the request thread
    // waits for a lock which the message thread holds.
    private static void deliverWithLookupBeforeTheProtocolExists(Node receiver,
                                                                 BisqEasyTakeOfferRequest request,
                                                                 BisqEasyTradeMessage message) throws Exception {
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
        }).when(receiver.protocolRegistry).find(message.getTradeId());

        TestThread messageThread = new TestThread(MESSAGE_THREAD, () -> receiver.service.onMessage(message));
        assertThat(lookupFoundNoProtocol.await(10, SECONDS)).isTrue();
        assertThat(receiver.protocolRegistry.hasPendingMessages())
                .as("the message thread stops between its lookup and the decision that the message is pending")
                .isFalse();

        TestThread requestThread = new TestThread(REQUEST_THREAD, () -> receiver.service.onMessage(request));
        awaitFinishedOrWaitingFor(requestThread.thread, messageThread.thread);
        // A request thread which waits for the lock of the message thread must wait before it registers the
        // protocol. Otherwise the registration is not one step with the decision of the message thread.
        if (requestThread.thread.isAlive()) {
            assertThat(receiver.protocolRegistry.find(message.getTradeId()))
                    .as("the request thread waits before it registers the protocol")
                    .isEmpty();
        }
        continueMessageThread.countDown();

        messageThread.join();
        requestThread.join();
    }

    // The request thread stops right after the protocol of the trade was created, before the protocol handles the
    // request. The message thread finds the protocol and passes the message to it. Then the request thread continues.
    private static void deliverWithLookupAfterTheProtocolExists(Node receiver,
                                                                BisqEasyTakeOfferRequest request,
                                                                BisqEasyTradeMessage message) throws Exception {
        CountDownLatch protocolCreated = new CountDownLatch(1);
        CountDownLatch continueRequestThread = new CountDownLatch(1);
        BisqEasyOpenTradeChannelService channelService =
                receiver.serviceProvider.getChatService().getBisqEasyOpenTradeChannelService();
        // The node processes the pending chat messages of a trade right after it created the protocol of the trade
        doAnswer(invocation -> {
            if (REQUEST_THREAD.equals(Thread.currentThread().getName())) {
                protocolCreated.countDown();
                assertThat(continueRequestThread.await(10, SECONDS)).isTrue();
            }
            return null;
        }).when(channelService).processPendingMessages(message.getTradeId());

        TestThread requestThread = new TestThread(REQUEST_THREAD, () -> receiver.service.onMessage(request));
        assertThat(protocolCreated.await(10, SECONDS)).isTrue();
        assertThat(receiver.service.findProtocol(message.getTradeId())).isPresent();

        TestThread messageThread = new TestThread(MESSAGE_THREAD, () -> receiver.service.onMessage(message));
        messageThread.join();
        assertThat(receiver.protocolRegistry.hasPendingMessages()).isFalse();
        continueRequestThread.countDown();
        requestThread.join();
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

    // A started thread whose failure the test reports
    private static final class TestThread {
        private final Thread thread;
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        private TestThread(String name, Runnable task) {
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

    // The maker of a buy offer is the buyer
    private static BisqEasyOffer buyOffer(NetworkId makerNetworkId) {
        return new BisqEasyOffer("buy-offer",
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.BUY,
                MarketRepository.getUSDBitcoinMarket(),
                new QuoteSideFixedAmountSpec(QUOTE_AMOUNT),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                1,
                "1.0.0",
                "2.1.13");
    }

    // The seller took the buy offer and sent the payment account data. Nothing was delivered yet.
    private final class SellerTakesBuyOffer {
        private final Identity buyerIdentity = identity("buyer-as-maker");
        private final Identity sellerIdentity = identity("seller-as-taker");
        private final BisqEasyOffer offer = buyOffer(buyerIdentity.getNetworkId());
        private final BisqEasyContract contract = contract(offer, sellerIdentity.getNetworkId(), Optional.empty());
        private final BisqEasyTrade sellersTrade = new BisqEasyTrade(contract, false, true, sellerIdentity, offer,
                sellerIdentity.getNetworkId(), buyerIdentity.getNetworkId());
        private final Node buyer = new Node();
        private final Node seller = new Node();

        private SellerTakesBuyOffer() {
            makerAcceptsTakeOfferRequests(buyer.serviceProvider, buyerIdentity, offer);
            // As buyer, the maker checks the reputation of the selling taker
            when(buyer.serviceProvider.getUserService().getReputationService()
                    .getReputationScore(sellerIdentity.getNetworkId().getId()))
                    .thenReturn(new ReputationScore(1_000_000, 5, 1));
            // The maker's node creates its trade from the request. This copy only provides the data of the channel.
            buyer.hasChannelOf(new BisqEasyTrade(contract, true, false, buyerIdentity, offer,
                    sellerIdentity.getNetworkId(), buyerIdentity.getNetworkId()), sellerIdentity);
            buyer.start();

            seller.hasChannelOf(sellersTrade, buyerIdentity);
            seller.start(sellersTrade);
            seller.service.takeOffer(sellersTrade);
            seller.service.sellerSendsPaymentAccount(sellersTrade, ACCOUNT_DATA);
        }

        private BisqEasyTrade buyersTrade() {
            return buyer.service.findOpenTrade(sellersTrade.getId()).orElseThrow();
        }
    }

    // The buyer took the sell offer and sent the Bitcoin address. Nothing was delivered yet.
    private final class BuyerTakesSellOffer {
        private final Identity sellerIdentity = identity("seller-as-maker");
        private final Identity buyerIdentity = identity("buyer-as-taker");
        private final BisqEasyOffer offer = offer("sell-offer", sellerIdentity.getNetworkId(), QUOTE_AMOUNT);
        private final BisqEasyContract contract = contract(offer, buyerIdentity.getNetworkId(), Optional.empty());
        private final BisqEasyTrade buyersTrade = new BisqEasyTrade(contract, true, true, buyerIdentity, offer,
                buyerIdentity.getNetworkId(), sellerIdentity.getNetworkId());
        private final Node seller = new Node();
        private final Node buyer = new Node();

        private BuyerTakesSellOffer() {
            makerAcceptsTakeOfferRequests(seller.serviceProvider, sellerIdentity, offer);
            seller.hasChannelOf(new BisqEasyTrade(contract, false, false, sellerIdentity, offer,
                    buyerIdentity.getNetworkId(), sellerIdentity.getNetworkId()), buyerIdentity);
            seller.start();

            buyer.hasChannelOf(buyersTrade, sellerIdentity);
            buyer.start(buyersTrade);
            buyer.service.takeOffer(buyersTrade);
            buyer.service.buyerSendBitcoinPaymentData(buyersTrade, BTC_ADDRESS);
        }

        private BisqEasyTrade sellersTrade() {
            return seller.service.findOpenTrade(buyersTrade.getId()).orElseThrow();
        }
    }

    // A trade service whose network service keeps the sent trade messages until the test delivers them. The protocol
    // registry of the service is a spy, so a test can stop a thread at the protocol lookup.
    private final class Node {
        private final ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        private final List<BisqEasyTradeMessage> sentMessages = new CopyOnWriteArrayList<>();
        private final BisqEasyProtocolRegistry protocolRegistry = spy(new BisqEasyProtocolRegistry());
        private BisqEasyTradeService service;

        private Node() {
            when(serviceProvider.getNetworkService().confidentialSend(any(), any(), any())).thenAnswer(invocation -> {
                if (invocation.getArgument(0) instanceof BisqEasyTradeMessage message) {
                    sentMessages.add(message);
                }
                return CompletableFuture.completedFuture(new SendMessageResult());
            });
            when(serviceProvider.getContractService()).thenReturn(new ContractService(mock(SecurityService.class)));
            when(serviceProvider.getSettingsService().getCloseMyOfferWhenTaken()).thenReturn(new Observable<>(false));
        }

        private void hasChannelOf(BisqEasyTrade trade, Identity peerIdentity) {
            when(serviceProvider.getChatService().getBisqEasyOpenTradeChannelService().findChannelByTradeId(trade.getId()))
                    .thenReturn(Optional.of(channel(trade, trade.getOffer(), userProfile(peerIdentity.getNetworkId()), Optional.empty())));
        }

        private void start(BisqEasyTrade... trades) {
            service = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP, protocolRegistry);
            nodes.add(this);
            when(serviceProvider.getBisqEasyTradeService()).thenReturn(service);
            for (BisqEasyTrade trade : trades) {
                service.getPersistableStore().addTrade(trade);
            }
            service.initialize().join();
        }

        private <M extends BisqEasyTradeMessage> M takeSentMessage(Class<M> type) {
            M message = sentMessages.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .findFirst()
                    .orElseThrow();
            sentMessages.remove(message);
            return message;
        }

        private void deliverSentMessagesTo(Node receiver) {
            while (!sentMessages.isEmpty()) {
                receiver.service.onMessage(sentMessages.remove(0));
            }
        }
    }
}
