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
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.fsm.FsmException;
import bisq.common.market.Market;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufEnumException;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.message.NetworkMessageResolver;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.trade.bisq_easy.protocol.BisqEasySellerAsMakerProtocol;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyBtcAddressMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyCancelTradeMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatSentMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.common.util.StringUtils;
import bisq.trade.protocol.messages.TradeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Coverage for the stuck-trade fix (bisq-mobile#1622 / #4885): out-of-order protocol messages queued by the
 * FSM survive persistence round-trips and drain on restore (in-memory, via the real service lifecycle and via
 * real disk persistence), the startup register-before-replay handoff loses no message, take-offer creation is
 * duplicate-safe and atomic (sequential and concurrent), final states persist unthrottled, and the restore
 * drain honours redaction, banned-sender and trading-halt guards.
 */
class BisqEasyTradeTest {
    static {
        // Normally registered once at app startup by bisq.application.ResolverConfig, which is not a dependency
        // available to :trade tests. Required so that a persisted, wrapped TradeMessage (see
        // Trade#pendingEventsFromProto) can be resolved back via EnvelopePayloadMessage.fromProto() -> Any.unpack().
        NetworkMessageResolver.addResolver("trade.TradeMessage", TradeMessage.getNetworkMessageResolver());
    }

    /**
     * End-to-end reproduction of issue #1622: a Bisq Easy trade getting stuck because a message which arrives
     * out of order (here: the buyer's confirm-fiat-sent message, before the seller has processed the buyer's
     * btc-address message) sits in the FSM's event queue, which historically was not persisted and therefore
     * lost across a restart. Drives the real seller-as-maker protocol/FSM and the real Trade proto (de)serialization,
     * proving the queued event survives a toProto()/fromProto() round trip and is correctly re-applied once the
     * enabling message arrives on the restored trade - without needing the fiat-sent message to be re-delivered.
     */
    @Test
    void confirmFiatSentMessageQueuedWhileBtcAddressPendingSurvivesRestoreAndDrains() throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker");
        NetworkId makerNetworkId = createNetworkId("seller-maker");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);
        ServiceProvider serviceProvider = createServiceProvider();

        // Seller-as-maker trade, hand-placed at the state reached after: seller sent account data, but has not
        // yet processed the buyer's btc-address message. This is the realistic predecessor state for #1622: the
        // buyer cannot confirm fiat-sent before having received the seller's account data (see BisqEasyBuyerAsTakerProtocol),
        // so by the time the buyer's confirm-fiat-sent message reaches the seller, SELLER_SENT_ACCOUNT_DATA must
        // already be true; the only leg genuinely still in flight is the buyer's own btc-address message.
        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        // The trade ID is derived (Trade#createId) from the offer/taker/take-offer-date, not chosen by us -
        // messages must be addressed to the real ID or TradeMessageHandler#verifyInternal rejects them.
        String tradeId = trade.getId();

        BisqEasySellerAsMakerProtocol protocol = new BisqEasySellerAsMakerProtocol(serviceProvider, trade);

        BisqEasyConfirmFiatSentMessage fiatSentMessage = new BisqEasyConfirmFiatSentMessage(
                "fiat-sent-msg", tradeId, protocol.getVersion(), takerNetworkId, makerNetworkId);

        // The message arrives before its prerequisite transition: no matching transition from the current
        // state, so the Fsm queues it instead of applying it.
        protocol.handle(fiatSentMessage);
        assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                trade.getTradeState());
        assertEquals(1, trade.getEventQueue().size());

        // Restart: round-trip the trade through proto exactly as persistence does.
        bisq.trade.protobuf.Trade proto = trade.toProto(false);
        BisqEasyTrade restoredTrade = BisqEasyTrade.fromProto(proto);

        // The queued event survives the restart.
        assertEquals(1, restoredTrade.getEventQueue().size());
        assertTrue(restoredTrade.getEventQueue().stream().anyMatch(BisqEasyConfirmFiatSentMessage.class::isInstance));
        assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                restoredTrade.getTradeState());

        BisqEasySellerAsMakerProtocol restoredProtocol = new BisqEasySellerAsMakerProtocol(serviceProvider, restoredTrade);

        // Draining right after restore is a safe no-op: the current state still does not accept the queued event.
        restoredProtocol.drainEventQueue();
        assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                restoredTrade.getTradeState());

        // The genuinely pending message (buyer's btc address) finally arrives.
        BisqEasyBtcAddressMessage btcAddressMessage = new BisqEasyBtcAddressMessage(
                "btc-address-msg", tradeId, restoredProtocol.getVersion(), takerNetworkId, makerNetworkId,
                "bc1qxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzx", offer);
        restoredProtocol.handle(btcAddressMessage);

        // This transition succeeds and automatically drains the restored queue, re-applying the previously
        // queued confirm-fiat-sent message - the trade is no longer stuck, with no restart-timing luck required.
        assertEquals(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, restoredTrade.getTradeState());
        assertTrue(restoredTrade.getEventQueue().isEmpty());
    }

    /**
     * End-to-end regression test for issue #1622, driven exclusively through {@link BisqEasyTradeService}'s
     * public API - {@link BisqEasyTradeService#onMessage(EnvelopePayloadMessage)}, the real
     * {@code ConfidentialMessageService.Listener} entry point for every inbound trade message, and
     * {@link BisqEasyTradeService#initialize()}, the real app-bootstrap entry point that restores persisted
     * trades - rather than by calling {@code protocol.handle()}/{@code drainEventQueue()} directly as
     * {@link #confirmFiatSentMessageQueuedWhileBtcAddressPendingSurvivesRestoreAndDrains()} above does. This
     * proves the fix survives the actual node lifecycle, not just the Fsm/Trade unit-level mechanics.
     * <p>
     * Phase 1 ("before restart"): a seller-as-maker trade is hand-placed at the state reached after the seller
     * has sent its account data but not yet received the buyer's btc-address message. A
     * {@link BisqEasyTradeService} instance registers the trade's protocol via {@code initialize()} (as a
     * running node's service would on startup), then receives the buyer's confirm-fiat-sent message via
     * {@code onMessage()} - out of order, since the btc-address leg is still outstanding, so it is queued
     * rather than applied.
     * <p>
     * Phase 2 (restart): the trade is round-tripped through {@code toProto()}/{@code fromProto()}, exactly as
     * persistence does across a restart. Pre-fix, the queued event is silently dropped here.
     * <p>
     * Phase 3 ("after restart"): a brand-new {@link BisqEasyTradeService} instance (a fresh service/Fsm, as a
     * genuine restart produces - not the same instance from phase 1) registers the restored trade's protocol
     * via {@code initialize()}, then the genuinely still-pending btc-address message arrives via
     * {@code onMessage()}. On the fix, {@code initialize()}'s restore drain plus the Fsm's own post-transition
     * auto-drain re-applies the previously queued confirm-fiat-sent message once the btc-address transition
     * succeeds, so the trade reaches {@code SELLER_RECEIVED_FIAT_SENT_CONFIRMATION} without the peer needing to
     * resend anything. Pre-fix, the trade gets stuck one step short, at
     * {@code MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS}.
     */
    @Test
    void confirmFiatSentMessageQueuedWhileBtcAddressPendingSurvivesRestoreAndDrainsViaRealServiceLifecycle(@TempDir Path tempDir)
            throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-lifecycle");
        NetworkId makerNetworkId = createNetworkId("seller-maker-lifecycle");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        String tradeId = trade.getId();

        // Phase 1: "before restart" - a running node's service instance registers the trade's protocol exactly
        // as a real node does on startup (initialize()), then a real inbound message is routed through the
        // real ConfidentialMessageService.Listener entry point (onMessage()), never via protocol.handle().
        LifecycleHarness harnessA = createLifecycleHarness(tempDir.resolve("before-restart"));
        BisqEasyTradeService tradeServiceA = harnessA.tradeService();
        try {
            tradeServiceA.getPersistableStore().addTrade(trade);
            tradeServiceA.initialize();

            BisqEasyConfirmFiatSentMessage fiatSentMessage = new BisqEasyConfirmFiatSentMessage(
                    "fiat-sent-msg", tradeId, BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId);
            tradeServiceA.onMessage(fiatSentMessage);

            // Diagnostic only: confirms the message really did get queued rather than applied (pre-existing,
            // unchanged-by-the-fix queuing behavior) - not itself the regression signal.
            assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                    trade.getTradeState());
        } finally {
            // Drain the shared persistence executor before this test's @TempDir is cleaned: the service's
            // persist() calls are fire-and-forget async writes, and a write landing after cleanup makes
            // JUnit's temp-dir deletion fail (flaky "Failed to close extension context"). persistNow()'s
            // future queues behind every earlier write on the FIFO executor, so joining it = quiescence.
            tradeServiceA.persistNow().join();
            tradeServiceA.shutdown();
        }

        // Phase 2: restart - the exact persistence round trip.
        bisq.trade.protobuf.Trade proto = trade.toProto(false);
        BisqEasyTrade restoredTrade = BisqEasyTrade.fromProto(proto);

        // Phase 3: "after restart" - a brand-new service/Fsm (never harnessA/tradeServiceA - a real restart
        // produces a brand-new process/service), registers the restored trade via initialize() (the real
        // app-bootstrap entry point), then the genuinely still-pending btc-address message arrives via
        // onMessage().
        LifecycleHarness harnessB = createLifecycleHarness(tempDir.resolve("after-restart"));
        BisqEasyTradeService tradeServiceB = harnessB.tradeService();
        tradeServiceB.getPersistableStore().addTrade(restoredTrade);
        tradeServiceB.initialize();

        BisqEasyBtcAddressMessage btcAddressMessage = new BisqEasyBtcAddressMessage(
                "btc-address-msg", tradeId, BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId,
                "bc1qxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzx", offer);
        try {
            tradeServiceB.onMessage(btcAddressMessage);

            // The load-bearing assertion: on the fix, the previously queued confirm-fiat-sent message survived
            // the restart and was re-applied once the btc-address transition unblocked it - the trade is no
            // longer stuck, without the peer needing to resend anything. Pre-fix this fails, with the trade
            // stuck one step short at ..._SELLER_RECEIVED_BTC_ADDRESS.
            assertEquals(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, restoredTrade.getTradeState());
        } finally {
            // Drain the shared persistence executor before this test's @TempDir is cleaned: the service's
            // persist() calls are fire-and-forget async writes, and a write landing after cleanup makes
            // JUnit's temp-dir deletion fail (flaky "Failed to close extension context"). persistNow()'s
            // future queues behind every earlier write on the FIFO executor, so joining it = quiescence.
            tradeServiceB.persistNow().join();
            tradeServiceB.shutdown();
        }
    }

    /**
     * Startup handoff: a message the network finishes processing after initialize()'s replay has read the
     * processed-messages snapshot, but before the service's live listener is registered, must still reach the
     * trade FSM. The network layer ACKs such a message and removes it from the mailbox, so if it falls into
     * this window it is gone for good - the replay never saw it and no live delivery happened. The emulated
     * ConfidentialMessageService below reproduces that timeline deterministically: its processed-messages
     * read dispatches the in-window message live ONLY if a listener is already registered, exactly like the
     * real network side. With replay-before-registration this fails (the trade silently never advances); with
     * registration-before-replay the live path picks the message up.
     */
    @Test
    void messageProcessedDuringStartupHandoffIsNotLost(@TempDir Path tempDir) throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-handoff");
        NetworkId makerNetworkId = createNetworkId("seller-maker-handoff");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        String tradeId = trade.getId();

        LifecycleHarness harness = createLifecycleHarness(tempDir.resolve("handoff"));
        BisqEasyTradeService tradeService = harness.tradeService();
        NetworkService networkService = harness.networkService();

        BisqEasyBtcAddressMessage btcAddressMessage = new BisqEasyBtcAddressMessage(
                "btc-address-msg-handoff", tradeId, BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId,
                "bc1qxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzx", offer);

        AtomicReference<ConfidentialMessageService.Listener> registeredListener = new AtomicReference<>();
        doAnswer(invocation -> {
            registeredListener.set(invocation.getArgument(0));
            return null;
        }).when(networkService).addConfidentialMessageListener(any());

        ConfidentialMessageService confidentialMessageService = mock(ConfidentialMessageService.class);
        when(confidentialMessageService.getProcessedEnvelopePayloadMessages()).thenAnswer(invocation -> {
            // The replay's snapshot is read first - the in-window message is NOT part of it...
            Set<EnvelopePayloadMessage> snapshot = Set.of();
            // ...and the network finishes processing it right in the handoff window: ACKed, removed from the
            // mailbox, and dispatched live only if a listener is registered by now.
            ConfidentialMessageService.Listener listener = registeredListener.get();
            if (listener != null) {
                listener.onMessage(btcAddressMessage);
            }
            return snapshot;
        });
        when(networkService.getConfidentialMessageServices()).thenReturn(Set.of(confidentialMessageService));

        tradeService.getPersistableStore().addTrade(trade);
        try {
            tradeService.initialize();

            // The load-bearing assertion: the in-window message must have reached the FSM - the network will
            // never redeliver it. Pre-fix (replay before listener registration) the trade silently stays at
            // ..._SELLER_DID_NOT_RECEIVED_BTC_ADDRESS.
            assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
                    trade.getTradeState());
        } finally {
            // Drain the shared persistence executor before this test's @TempDir is cleaned: the service's
            // persist() calls are fire-and-forget async writes, and a write landing after cleanup makes
            // JUnit's temp-dir deletion fail (flaky "Failed to close extension context"). persistNow()'s
            // future queues behind every earlier write on the FIFO executor, so joining it = quiescence.
            tradeService.persistNow().join();
            tradeService.shutdown();
        }
    }

    /**
     * Strengthens the real-service-lifecycle regression above (Kim's #4885 review point) by exercising the
     * ACTUAL on-disk persistence round trip - {@link BisqEasyTradeService#persist()} (real
     * {@code RateLimitedPersistenceClient#persist()} -> {@code Persistence#persistAsync()/write()}), then
     * {@link BisqEasyTradeService#readPersisted()} (real {@code Persistence#read()}) - rather than a manual
     * {@code trade.toProto()}/{@code BisqEasyTrade.fromProto()} call. The previous version of this test (see
     * above) only proved the FSM/Trade proto (de)serialization survives a round trip, not that the actual
     * persistence write/read path a running node relies on does. This also exercises the #4885 follow-up fix
     * (the atomic {state, eventQueue} snapshot - see {@code FsmModel#getStateAndEventQueueSnapshot} /
     * {@code Trade#getTradeBuilder}): a real, asynchronously-scheduled {@code persistAsync()} write is exactly
     * the kind of cross-thread read a torn, unsynchronized snapshot could have corrupted.
     */
    @Test
    void confirmFiatSentMessageQueuedWhileBtcAddressPendingSurvivesRealDiskPersistenceRoundTripAndDrains(@TempDir Path persistenceDir)
            throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-disk");
        NetworkId makerNetworkId = createNetworkId("seller-maker-disk");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        String tradeId = trade.getId();

        // Phase 1 ("before restart"): same setup as the lifecycle test above, but bound to a real, on-disk
        // persistence directory that is deliberately reused (not a fresh subdirectory per phase) for the
        // "after restart" service below - a real restart reads the SAME data directory a previous run wrote to.
        LifecycleHarness harnessA = createLifecycleHarness(persistenceDir);
        BisqEasyTradeService tradeServiceA = harnessA.tradeService();
        try {
            tradeServiceA.getPersistableStore().addTrade(trade);
            tradeServiceA.initialize();

            BisqEasyConfirmFiatSentMessage fiatSentMessage = new BisqEasyConfirmFiatSentMessage(
                    "fiat-sent-msg-disk", tradeId, BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId);
            tradeServiceA.onMessage(fiatSentMessage);
            assertEquals(1, trade.getEventQueue().size());

            // Force a real write to disk and block until it has landed. Note: onMessage() above already triggered a
            // persist() internally (Fsm#handle()'s finally always persists after every transition, including queuing
            // an out-of-order event) - but RateLimitedPersistenceClient#persist() throttles to at most one write per
            // second, so a second, explicit call to persist() here could easily land inside that same throttling
            // window and be silently dropped without writing anything (this is precisely the kind of gap the #4885
            // follow-up on final-state persistence closes for completed trades - see persistNow()). Rather than racing
            // that throttle, we go around it via the lower-level Persistence#persistAsync() directly: it runs on the
            // same single-threaded write executor as every persist() call, so waiting for THIS write to complete
            // guarantees any earlier-submitted write (like the automatic one above) has already landed too (FIFO).
            tradeServiceA.getPersistence().persistAsync(tradeServiceA.getPersistableStore().getClone()).join();
        } finally {
            // Drain the shared persistence executor before this test's @TempDir is cleaned: the service's
            // persist() calls are fire-and-forget async writes, and a write landing after cleanup makes
            // JUnit's temp-dir deletion fail (flaky "Failed to close extension context"). persistNow()'s
            // future queues behind every earlier write on the FIFO executor, so joining it = quiescence.
            tradeServiceA.persistNow().join();
            tradeServiceA.shutdown();
        }

        // Phase 2 (restart): a brand-new service instance bound to the SAME persistence directory, reading back
        // via the real PersistenceClient#readPersisted() path - what PersistenceService#readAllPersisted() calls
        // for every registered client at real app bootstrap - instead of a manual toProto()/fromProto() round trip.
        LifecycleHarness harnessB = createLifecycleHarness(persistenceDir);
        BisqEasyTradeService tradeServiceB = harnessB.tradeService();
        assertTrue(tradeServiceB.readPersisted().isPresent());

        BisqEasyTrade restoredTrade = tradeServiceB.findTrade(tradeId).orElseThrow();
        assertEquals(1, restoredTrade.getEventQueue().size());
        assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                restoredTrade.getTradeState());

        // Phase 3 ("after restart"): initialize() registers the restored trade's protocol and attempts a drain
        // (a no-op here, since the still-missing btc-address leg has not arrived yet), then the genuinely
        // pending message arrives via the real onMessage() entry point.
        tradeServiceB.initialize();

        BisqEasyBtcAddressMessage btcAddressMessage = new BisqEasyBtcAddressMessage(
                "btc-address-msg-disk", tradeId, BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId,
                "bc1qxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzx", offer);
        try {
            tradeServiceB.onMessage(btcAddressMessage);

            // The load-bearing assertion: on the fix, the previously queued confirm-fiat-sent message survived a
            // REAL disk write + read and was re-applied once the btc-address transition unblocked it. Pre-fix
            // this fails, with the trade stuck one step short at ..._SELLER_RECEIVED_BTC_ADDRESS.
            assertEquals(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, restoredTrade.getTradeState());
        } finally {
            // Drain the shared persistence executor before this test's @TempDir is cleaned: the service's
            // persist() calls are fire-and-forget async writes, and a write landing after cleanup makes
            // JUnit's temp-dir deletion fail (flaky "Failed to close extension context"). persistNow()'s
            // future queues behind every earlier write on the FIFO executor, so joining it = quiescence.
            tradeServiceB.persistNow().join();
            tradeServiceB.shutdown();
        }
    }

    /**
     * #4885 follow-up (privacy/retention, Henrik's review point): reaching a final state must flush the
     * in-memory clear of eventQueue/processedEvents to disk via an unthrottled write
     * ({@code Fsm#persistOnFinalState} -> {@code BisqEasyProtocol#persistOnFinalState} ->
     * {@code BisqEasyTradeService#persistNow}), not the ordinary, rate-limitable {@code persist()} - a
     * throttled-and-dropped write here would leave a private, already-in-memory-wiped message stranded on disk
     * indefinitely, since a cancelled trade has no further transition to ever trigger a follow-up write. This is
     * verified as a deterministic interaction (which persistence method gets invoked, and how many times) rather
     * than by racing real disk-write completion timing.
     */
    @Test
    void reachingFinalStateForcesUnthrottledPersistRatherThanTheNormalRateLimitedOne() throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-cancel");
        NetworkId makerNetworkId = createNetworkId("seller-maker-cancel");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        BisqEasyTradeService mockTradeService = mock(BisqEasyTradeService.class);
        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getBisqEasyTradeService()).thenReturn(mockTradeService);

        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        String tradeId = trade.getId();
        BisqEasySellerAsMakerProtocol protocol = new BisqEasySellerAsMakerProtocol(serviceProvider, trade);

        // A non-final, out-of-order transition first: the fiat-sent message doesn't match a transition from the
        // current state, so it gets queued instead of applied. Its persist call must go through the ordinary,
        // rate-limitable path - a genuinely private message is now sitting parked in the queue.
        BisqEasyConfirmFiatSentMessage fiatSentMessage = new BisqEasyConfirmFiatSentMessage(
                "fiat-sent-msg-cancel", tradeId, protocol.getVersion(), takerNetworkId, makerNetworkId);
        protocol.handle(fiatSentMessage);
        assertEquals(1, trade.getEventQueue().size());
        verify(mockTradeService, times(1)).persist();
        verify(mockTradeService, never()).persistNow();

        // Cancel the trade via a real, message-driven transition to a final state (CANCELLED is reachable from
        // this exact state - see BisqEasySellerAsMakerProtocol's "Cancel trade" transition). Fsm#handle() clears
        // eventQueue/processedEvents in-memory here - the parked fiat-sent message included.
        BisqEasyCancelTradeMessage cancelTradeMessage = new BisqEasyCancelTradeMessage(
                "cancel-msg", tradeId, protocol.getVersion(), takerNetworkId, makerNetworkId);
        protocol.handle(cancelTradeMessage);

        assertEquals(BisqEasyTradeState.CANCELLED, trade.getTradeState());
        assertTrue(trade.getEventQueue().isEmpty());

        // The load-bearing assertions: the completing transition's persist call went through persistNow()
        // instead of persist() - persist()'s count is unchanged from before the cancel, and persistNow() fired
        // exactly once.
        verify(mockTradeService, times(1)).persist();
        verify(mockTradeService, times(1)).persistNow();
    }

    // NOTE on the buyer-as-taker "bonus" case (skipped - see final report for why): an initial attempt queued
    // BisqEasyConfirmFiatSentEvent early (the single-source-state transition immediately downstream of
    // BisqEasyBuyerAsTakerProtocol's analogous two-leg convergence) and round-tripped it through
    // toProto()/fromProto(), mirroring the seller test above. It failed identically on both the fix and
    // pre-fix code, because Trade#getTradeBuilder deliberately only serializes eventQueue entries that are
    // EnvelopePayloadMessage instances (i.e. network messages) - see the comment there: local, user-triggered
    // events like BisqEasyConfirmFiatSentEvent have no proto representation and are explicitly considered safe
    // to drop, since they only ever exist while the app is already running. So that scenario was never a valid
    // regression signal for #1622 to begin with. A genuinely analogous *message*-only race exists
    // (BisqEasyAccountDataMessage arriving before BisqEasyTakeOfferResponse - see
    // BisqEasyBuyerAsTakerProtocol#configTransitions "Option 2"), but BisqEasyTakeOfferResponseHandler#verify()
    // requires a real, hash-matching ContractSignatureData plus a ContractService that reports matching public
    // keys/valid signature, which the current lightweight ServiceProvider deep-stub does not provide - wiring
    // that up correctly is materially more effort than the seller case, so it is left as a follow-up.

    /**
     * Creation-atomicity regression (Kim's #4885 follow-up finding): registering the live listener before the
     * startup replay (see initialize()) makes duplicate delivery of the very same take-offer request expected,
     * not exceptional. Two deliveries for a trade id that does not exist yet must not each create their own
     * trade+protocol - that would be a split brain where one protocol instance is silently orphaned and any
     * handler side effect (e.g. sending the take-offer response) runs twice.
     * <br/>
     * Drives {@link BisqEasyTradeService#makerCreatesProtocol} directly, sequentially, on one thread - this
     * isolates the check-then-act creation guard from {@code protocol.handle()}'s handler-side signature
     * verification, which needs a fully wired contract-signing chain the lightweight harness below does not
     * provide (see the "bonus case" note above). The concurrent variant below additionally proves the guard
     * holds under a genuine race, not just sequentially.
     */
    @Test
    void secondMakerCreatesProtocolCallForSameContractIsRejectedWithoutOrphaningTheFirst(@TempDir Path tempDir) {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-dup");
        NetworkId makerNetworkId = createNetworkId("seller-maker-dup");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        LifecycleHarness harness = createLifecycleHarness(tempDir);
        when(harness.serviceProvider().getIdentityService().findAnyIdentityByNetworkId(makerNetworkId))
                .thenReturn(Optional.of(createIdentity(makerNetworkId)));
        BisqEasyTradeService tradeService = harness.tradeService();
        try {
            BisqEasyProtocol firstProtocol = tradeService.makerCreatesProtocol(contract, takerNetworkId, makerNetworkId);
            String tradeId = tradeService.getTrades().stream().findAny().orElseThrow().getId();

            IllegalArgumentException secondCallFailure = assertThrows(IllegalArgumentException.class,
                    () -> tradeService.makerCreatesProtocol(contract, takerNetworkId, makerNetworkId),
                    "a second creation attempt for the same contract must be rejected, not create a second trade");
            assertEquals("We received the BisqEasyTakeOfferRequest for an already existing protocol",
                    secondCallFailure.getMessage());

            assertEquals(1, tradeService.getTrades().size());
            assertSame(firstProtocol, tradeService.findProtocol(tradeId).orElseThrow(),
                    "the rejected second call must not replace or orphan the first protocol instance");
        } finally {
            tradeService.persistNow().join();
            tradeService.shutdown();
        }
    }

    /**
     * Concurrent variant of the creation-atomicity guard above, using a latch-gated seam
     * ({@link BisqEasyTradeService#awaitCreationTestSeam}) to deterministically force the exact interleaving
     * the fix guards against, rather than relying on scheduling luck - mirrors the GatedSerializationStore /
     * BlockingCloneStore latch pattern in the persistence test suite (see PersistenceWriteGuardTests /
     * RateLimitedPersistenceClientTests). Thread A is parked - still holding creationLock - between the "no
     * duplicate exists" checks and the writes that make the trade/protocol exist; thread B is then released
     * and attempts to create a protocol for the very same contract while A is still parked.
     * <br/>
     * This test was run against the pre-fix code (creationLock removed, i.e. makerCreatesProtocol's body
     * unguarded) to confirm it goes red: both threads pass the checks concurrently and each create their own
     * trade+protocol for the same id (two trades, two protocols, no exception on either side) - exactly the
     * split brain the fix closes. With creationLock in place, thread B blocks until thread A's whole creation
     * finishes, then correctly observes A's already-created trade/protocol and is rejected.
     */
    @Test
    void concurrentMakerCreatesProtocolCallsForSameContractCreateExactlyOneTradeAndProtocol(@TempDir Path tempDir) throws Exception {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-race");
        NetworkId makerNetworkId = createNetworkId("seller-maker-race");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getConfidentialMessageServices()).thenReturn(Set.of());
        when(networkService.confidentialSend(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendMessageResult.class)));
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getNetworkService()).thenReturn(networkService);
        when(serviceProvider.getPersistenceService()).thenReturn(new PersistenceService(tempDir));
        when(serviceProvider.getIdentityService().findAnyIdentityByNetworkId(makerNetworkId))
                .thenReturn(Optional.of(createIdentity(makerNetworkId)));

        CountDownLatch threadAEnteredSeam = new CountDownLatch(1);
        CountDownLatch releaseThreadA = new CountDownLatch(1);
        AtomicReference<Thread> firstThreadToEnterSeam = new AtomicReference<>();
        BisqEasyTradeService tradeService = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP) {
            @Override
            void awaitCreationTestSeam(String tradeId) {
                if (firstThreadToEnterSeam.compareAndSet(null, Thread.currentThread())) {
                    threadAEnteredSeam.countDown();
                    try {
                        if (!releaseThreadA.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("thread A was never released");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        when(serviceProvider.getBisqEasyTradeService()).thenReturn(tradeService);

        try {
            AtomicReference<BisqEasyProtocol> protocolA = new AtomicReference<>();
            AtomicReference<Throwable> failureA = new AtomicReference<>();
            Thread threadA = new Thread(() -> {
                try {
                    protocolA.set(tradeService.makerCreatesProtocol(contract, takerNetworkId, makerNetworkId));
                } catch (Throwable t) {
                    failureA.set(t);
                }
            }, "creation-thread-A");
            threadA.start();
            assertTrue(threadAEnteredSeam.await(5, TimeUnit.SECONDS),
                    "thread A must reach the seam while holding creationLock");

            AtomicReference<BisqEasyProtocol> protocolB = new AtomicReference<>();
            AtomicReference<Throwable> failureB = new AtomicReference<>();
            Thread threadB = new Thread(() -> {
                try {
                    protocolB.set(tradeService.makerCreatesProtocol(contract, takerNetworkId, makerNetworkId));
                } catch (Throwable t) {
                    failureB.set(t);
                }
            }, "creation-thread-B");
            threadB.start();
            // With the fix, B must block trying to acquire creationLock (still held by A) - it cannot even
            // reach its own checks yet. This is the load-bearing difference from the unfixed code, where B
            // would run straight through and create its own trade+protocol while A is still parked.
            threadB.join(500);
            assertTrue(threadB.isAlive(),
                    "with the fix, thread B must block on creationLock while thread A is parked mid-creation");

            releaseThreadA.countDown();
            threadA.join(5_000);
            threadB.join(5_000);
            assertTrue(!threadA.isAlive() && !threadB.isAlive(), "both threads must have finished");

            assertNull(failureA.get(), "the winning creation must not fail");
            assertNotNull(protocolA.get());
            assertNotNull(failureB.get(), "the losing delivery must be rejected, not silently create a second trade/protocol");
            assertInstanceOf(IllegalArgumentException.class, failureB.get());

            assertEquals(1, tradeService.getTrades().size());
            String tradeId = tradeService.getTrades().stream().findAny().orElseThrow().getId();
            assertSame(protocolA.get(), tradeService.findProtocol(tradeId).orElseThrow(),
                    "exactly one protocol instance must be registered - the loser must not have orphaned it");
        } finally {
            tradeService.persistNow().join();
            tradeService.shutdown();
        }
    }

    /**
     * onMessage()-level sequential double-delivery of the very same take-offer request, driven through the
     * real {@link BisqEasyTradeService#onMessage(EnvelopePayloadMessage)} entry point rather than
     * {@code makerCreatesProtocol} directly. The constructed {@link BisqEasyTakeOfferRequest} cannot pass
     * {@code BisqEasyTakeOfferRequestHandler#verify()}'s real signature/contract checks - that needs a fully
     * wired {@code ContractService} (see the "bonus case" note above) - so what exactly happens inside
     * {@code protocol.handle()} on either delivery (a verification failure wrapped as {@link FsmException}, an
     * internal error-transition to a final state, or - as observed with this lightweight deep-stub harness - a
     * silent no-op once the FSM reaches a state that later guards against further transitions) is a known
     * harness limitation, not the regression signal, and is deliberately NOT asserted on here.
     * <br/>
     * The regression signal is creation-atomicity, which does not depend on the handler ever succeeding or on
     * which of those outcomes occurs: makerCreatesProtocol's checks/writes complete (or the fast path finds
     * the already-created protocol) strictly BEFORE protocol.handle() runs, so nothing that happens inside the
     * handler can unwind or duplicate the creation. Because the first delivery's creation succeeds regardless
     * of what happens afterwards, the SECOND delivery takes the lock-free fast path in
     * handleBisqEasyTakeOfferMessage (a protocol already exists for this trade id) rather than reaching
     * makerCreatesProtocol's checkArgument guard at all - so neither delivery can throw the "already existing
     * protocol" rejection here. The concurrent test above is what exercises the checkArgument/creationLock
     * backstop for a trade id that does not exist yet on either side.
     */
    @Test
    void sequentialDuplicateTakeOfferRequestDeliveryCreatesExactlyOneTradeAndProtocol(@TempDir Path tempDir) {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-onmsg-dup");
        NetworkId makerNetworkId = createNetworkId("seller-maker-onmsg-dup");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        LifecycleHarness harness = createLifecycleHarness(tempDir);
        when(harness.serviceProvider().getIdentityService().findAnyIdentityByNetworkId(makerNetworkId))
                .thenReturn(Optional.of(createIdentity(makerNetworkId)));
        BisqEasyTradeService tradeService = harness.tradeService();
        try {
            tradeService.initialize();

            BisqEasyTakeOfferRequest takeOfferRequest = createTakeOfferRequest(contract, takerNetworkId, makerNetworkId);

            // Whatever protocol.handle() does internally on either delivery is not asserted on - see the
            // javadoc above. Only the creation-atomicity contract is load-bearing here.
            catchThrowable(() -> tradeService.onMessage(takeOfferRequest));
            assertEquals(1, tradeService.getTrades().size(),
                    "creation must have completed regardless of what the handler call did afterwards");
            String tradeId = tradeService.getTrades().stream().findAny().orElseThrow().getId();
            BisqEasyProtocol protocolAfterFirstDelivery = tradeService.findProtocol(tradeId).orElseThrow();

            Throwable secondDeliveryFailure = catchThrowable(() -> tradeService.onMessage(takeOfferRequest));
            // The one thing the second delivery must NOT do is throw the creation-rejection exception - that
            // would mean it wrongly fell through to makerCreatesProtocol instead of the lock-free fast path.
            if (secondDeliveryFailure != null) {
                assertNotEquals("We received the BisqEasyTakeOfferRequest for an already existing protocol",
                        secondDeliveryFailure.getCause() == null ? null : secondDeliveryFailure.getCause().getMessage());
            }

            assertEquals(1, tradeService.getTrades().size());
            assertSame(protocolAfterFirstDelivery, tradeService.findProtocol(tradeId).orElseThrow(),
                    "the redelivered message must not have replaced or orphaned the original protocol instance");
        } finally {
            tradeService.persistNow().join();
            tradeService.shutdown();
        }
    }

    private static Throwable catchThrowable(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private static BisqEasyTakeOfferRequest createTakeOfferRequest(BisqEasyContract contract,
                                                                    NetworkId takerNetworkId,
                                                                    NetworkId makerNetworkId) {
        // Deliberately NOT a real signature: BisqEasyTakeOfferRequestHandler#verify() will reject this (see
        // the class-level javadoc on the test using this helper) - only NetworkDataValidation's structural
        // constraints (hash/signature byte lengths) need to be satisfied for the message to construct at all.
        byte[] fakeContractHash = DigestUtil.hash("fake-contract-hash".getBytes(StandardCharsets.UTF_8));
        byte[] fakeSignature = new byte[70];
        ContractSignatureData fakeSignatureData = new ContractSignatureData(
                fakeContractHash, fakeSignature, takerNetworkId.getPubKey().getPublicKey());
        return new BisqEasyTakeOfferRequest(StringUtils.createUid(),
                BisqEasyTrade.createId(contract, takerNetworkId),
                BisqEasyProtocol.VERSION,
                takerNetworkId,
                makerNetworkId,
                contract,
                fakeSignatureData);
    }

    /**
     * Privacy/retention follow-up to #4885 (Henrik's review point): the persisted out-of-order events hold the full
     * account-data network message, and a stuck trade may never reach a final state to clear them. This drives the
     * real retention pass over a stuck trade whose take-offer date is past the not-completed redaction threshold and
     * asserts the parked event is scrubbed - so the sensitive payload cannot linger on disk indefinitely.
     */
    @Test
    void redactionScrubsParkedOutOfOrderEventsOfStuckTrade(@TempDir Path tempDir)
            throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-redact");
        NetworkId makerNetworkId = createNetworkId("seller-maker-redact");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        // Take-offer date well past the 45-90 day not-completed redaction window so the pass acts on this trade.
        long oldTakeOfferDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(200);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId, oldTakeOfferDate);

        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        String tradeId = trade.getId();

        LifecycleHarness harness = createLifecycleHarness(tempDir);
        BisqEasyTradeService tradeService = harness.tradeService();
        try {
            tradeService.getPersistableStore().addTrade(trade);
            tradeService.initialize();

            // Park an out-of-order confirm-fiat-sent message (the stuck-trade shape) - it carries account data.
            BisqEasyConfirmFiatSentMessage fiatSentMessage = new BisqEasyConfirmFiatSentMessage(
                    "fiat-sent-msg-redact", tradeId, BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId);
            tradeService.onMessage(fiatSentMessage);
            assertEquals(1, trade.getEventQueue().size());

            // numDays is clamped to [45, 90] for not-completed trades; any value works since the trade is 200 days old.
            when(harness.serviceProvider().getSettingsService().getNumDaysAfterRedactingTradeData().get())
                    .thenReturn(90);

            tradeService.maybeRedactDataOfCompletedTrades();

            assertTrue(trade.getEventQueue().isEmpty(),
                    "The retention pass must scrub the parked out-of-order event of a stuck, past-threshold trade");
        } finally {
            // Drain the shared persistence executor before this test's @TempDir is cleaned: the service's
            // persist() calls are fire-and-forget async writes, and a write landing after cleanup makes
            // JUnit's temp-dir deletion fail (flaky "Failed to close extension context"). persistNow()'s
            // future queues behind every earlier write on the FIFO executor, so joining it = quiescence.
            tradeService.persistNow().join();
            tradeService.shutdown();
        }
    }

    /**
     * A queued event passed onMessage()'s guards when it was originally
     * received, but its sender may have been BANNED before the restart. The restore drain applies queued events
     * directly through the FSM - bypassing onMessage()'s banned-sender check - so the guard in
     * {@link BisqEasyTradeService#createAndAddTradeProtocol} must scrub such events before draining, mirroring
     * what onMessage() would do with the same message arriving live. The trade is placed at a state that
     * ACCEPTS the queued message, so an unguarded drain WOULD have applied it - the assertion that the state
     * did NOT advance is what separates "dropped" from "vacuously not applied".
     */
    @Test
    void restoreDrainDropsQueuedEventsFromNowBannedSender(@TempDir Path tempDir)
            throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-banned");
        NetworkId makerNetworkId = createNetworkId("seller-maker-banned");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        BisqEasyTrade restoredTrade =
                createRestoredTradeWithQueuedFiatSentMessageAtAcceptingState(contract, offer, takerNetworkId, makerNetworkId);

        LifecycleHarness harness = createLifecycleHarness(tempDir);
        // The sender of the queued message got banned between original receipt and the restart.
        when(harness.serviceProvider().getUserService().getBannedUserService()
                .isUserProfileBanned(any(NetworkId.class))).thenReturn(true);
        BisqEasyTradeService tradeService = harness.tradeService();
        try {
            tradeService.getPersistableStore().addTrade(restoredTrade);
            tradeService.initialize();

            // Dropped, not applied: queue empty AND state unchanged. (A drain would have advanced the state to
            // SELLER_RECEIVED_FIAT_SENT_CONFIRMATION - see the control phase of the halt test below.)
            assertTrue(restoredTrade.getEventQueue().isEmpty(),
                    "queued events from a banned sender must be scrubbed before the restore drain");
            assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
                    restoredTrade.getTradeState(),
                    "a banned sender's queued message must not be applied");
        } finally {
            // Drain the shared persistence executor before this test's @TempDir is cleaned: the service's
            // persist() calls are fire-and-forget async writes, and a write landing after cleanup makes
            // JUnit's temp-dir deletion fail (flaky "Failed to close extension context"). persistNow()'s
            // future queues behind every earlier write on the FIFO executor, so joining it = quiescence.
            tradeService.persistNow().join();
            tradeService.shutdown();
        }
    }

    /**
     * With an EMERGENCY halt-trading alert active (persisted alert data is
     * replayed synchronously when initialize() registers its observer - which now happens BEFORE the restore),
     * the restore drain must be DEFERRED: events stay queued rather than being applied or dropped, and drain on
     * a later restart once the alert is lifted. Phase 2 is that later restart and doubles as the positive
     * control proving the setup is real: without the alert, the same still-queued event drains and advances the
     * trade.
     */
    @Test
    void restoreDrainIsDeferredWhileTradingIsHaltedAndRunsOnceAlertIsLifted(@TempDir Path tempDir)
            throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-halt");
        NetworkId makerNetworkId = createNetworkId("seller-maker-halt");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        BisqEasyTrade restoredTrade =
                createRestoredTradeWithQueuedFiatSentMessageAtAcceptingState(contract, offer, takerNetworkId, makerNetworkId);

        // Phase 1: restart while a halt-trading emergency alert is in effect.
        LifecycleHarness haltedHarness = createLifecycleHarness(tempDir.resolve("halted"));
        AuthorizedAlertData haltAlert = mock(AuthorizedAlertData.class);
        when(haltAlert.getAlertType()).thenReturn(AlertType.EMERGENCY);
        when(haltAlert.getAppType()).thenReturn(AppType.DESKTOP);
        when(haltAlert.isHaltTrading()).thenReturn(true);
        when(haltedHarness.serviceProvider().getBondedRolesService().getAlertService().getAuthorizedAlertDataSet())
                .thenReturn(new ObservableSet<>(Set.of(haltAlert)));
        BisqEasyTradeService haltedTradeService = haltedHarness.tradeService();
        try {
            haltedTradeService.getPersistableStore().addTrade(restoredTrade);
            haltedTradeService.initialize();

            // Deferred: kept queued (not dropped) and not applied.
            assertEquals(1, restoredTrade.getEventQueue().size(),
                    "the queued event must survive a halted restart untouched");
            assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
                    restoredTrade.getTradeState(),
                    "no queued event may be applied while trading is halted");
        } finally {
            // Drain the shared persistence executor before this test's @TempDir is cleaned: the service's
            // persist() calls are fire-and-forget async writes, and a write landing after cleanup makes
            // JUnit's temp-dir deletion fail (flaky "Failed to close extension context"). persistNow()'s
            // future queues behind every earlier write on the FIFO executor, so joining it = quiescence.
            haltedTradeService.persistNow().join();
            haltedTradeService.shutdown();
        }

        // Phase 2: a later restart with the alert lifted - the still-queued event now drains and the trade
        // advances. This is also the positive control for both guard tests: it proves the chosen state really
        // does accept the queued message, so phase 1's non-application was the guard, not a vacuous no-op.
        LifecycleHarness liftedHarness = createLifecycleHarness(tempDir.resolve("lifted"));
        BisqEasyTradeService liftedTradeService = liftedHarness.tradeService();
        try {
            liftedTradeService.getPersistableStore().addTrade(restoredTrade);
            liftedTradeService.initialize();

            assertEquals(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, restoredTrade.getTradeState(),
                    "once the alert is lifted the deferred event must drain on the next restart");
            assertTrue(restoredTrade.getEventQueue().isEmpty());
        } finally {
            // Drain the shared persistence executor before this test's @TempDir is cleaned: the service's
            // persist() calls are fire-and-forget async writes, and a write landing after cleanup makes
            // JUnit's temp-dir deletion fail (flaky "Failed to close extension context"). persistNow()'s
            // future queues behind every earlier write on the FIFO executor, so joining it = quiescence.
            liftedTradeService.persistNow().join();
            liftedTradeService.shutdown();
        }
    }

    /**
     * Builds the restored-trade shape both guard tests need: a queued confirm-fiat-sent message, with the trade
     * state advanced (as part of the persistence round trip, as if the btc-address transition completed right
     * before shutdown without a drain) to the state that ACCEPTS the queued message - so the restore drain,
     * unless guarded, applies it immediately.
     */
    private static BisqEasyTrade createRestoredTradeWithQueuedFiatSentMessageAtAcceptingState(BisqEasyContract contract,
                                                                                              BisqEasyOffer offer,
                                                                                              NetworkId takerNetworkId,
                                                                                              NetworkId makerNetworkId)
            throws UnresolvableProtobufEnumException {
        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        BisqEasySellerAsMakerProtocol protocol = new BisqEasySellerAsMakerProtocol(createServiceProvider(), trade);
        BisqEasyConfirmFiatSentMessage fiatSentMessage = new BisqEasyConfirmFiatSentMessage(
                "fiat-sent-msg-guard", trade.getId(), protocol.getVersion(), takerNetworkId, makerNetworkId);
        // No transition from the current state accepts it -> parked in the event queue.
        protocol.handle(fiatSentMessage);
        assertEquals(1, trade.getEventQueue().size());

        bisq.trade.protobuf.Trade proto = trade.toProto(false).toBuilder()
                .setState(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS.name())
                .build();
        return BisqEasyTrade.fromProto(proto);
    }

    private record LifecycleHarness(BisqEasyTradeService tradeService, NetworkService networkService,
                                    ServiceProvider serviceProvider) {
    }

    // Builds a BisqEasyTradeService harness suitable for exercising the real initialize()/onMessage() lifecycle,
    // stubbing every ServiceProvider dependency initialize() itself touches: getConfidentialMessageServices(),
    // the alert/settings observers, and the periodic redaction scheduler.
    // Each call is given its own persistence directory: a real restart produces a brand-new service/Fsm, and
    // reusing one directory (or one service instance) across "before" and "after" phases would let the two
    // phases silently share state that only a real proto round trip should carry across.
    private static LifecycleHarness createLifecycleHarness(Path persistenceDir) {
        NetworkService networkService = mock(NetworkService.class);
        // Empty: message delivery is driven explicitly via onMessage() in the test body rather than letting
        // initialize()'s own startup replay (of whatever this would return) do it implicitly.
        when(networkService.getConfidentialMessageServices()).thenReturn(Set.of());
        when(networkService.confidentialSend(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendMessageResult.class)));

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getNetworkService()).thenReturn(networkService);
        when(serviceProvider.getPersistenceService()).thenReturn(new PersistenceService(persistenceDir));

        BisqEasyTradeService tradeService = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP);
        when(serviceProvider.getBisqEasyTradeService()).thenReturn(tradeService);

        return new LifecycleHarness(tradeService, networkService, serviceProvider);
    }

    private static BisqEasyTrade createTradeAtState(BisqEasyContract contract,
                                                     BisqEasyOffer offer,
                                                     NetworkId takerNetworkId,
                                                     NetworkId makerNetworkId,
                                                     BisqEasyTradeState state) throws UnresolvableProtobufEnumException {
        BisqEasyTrade freshTrade = new BisqEasyTrade(contract, false, false, createIdentity(makerNetworkId), offer,
                takerNetworkId, makerNetworkId);
        bisq.trade.protobuf.Trade proto = freshTrade.toProto(false).toBuilder()
                .setState(state.name())
                .build();
        return BisqEasyTrade.fromProto(proto);
    }

    private static Identity createIdentity(NetworkId networkId) {
        KeyBundle keyBundle = new KeyBundle("test-key-bundle",
                KeyGeneration.generateDefaultEcKeyPair(),
                TorKeyGeneration.generateKeyPair(),
                I2PKeyGeneration.generateKeyPair());
        return new Identity("test-id", networkId, keyBundle);
    }

    private static ServiceProvider createServiceProvider() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getBisqEasyTradeService()).thenReturn(mock(BisqEasyTradeService.class));
        return serviceProvider;
    }

    private static BisqEasyContract createRealContract(BisqEasyOffer offer, NetworkId takerNetworkId) {
        return createRealContract(offer, takerNetworkId, System.currentTimeMillis());
    }

    private static BisqEasyContract createRealContract(BisqEasyOffer offer, NetworkId takerNetworkId, long takeOfferDate) {
        return new BisqEasyContract(takeOfferDate,
                offer,
                takerNetworkId,
                100_000,
                3_500_000,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                Optional.empty(),
                new MarketPriceSpec(),
                0);
    }

    private static BisqEasyOffer createRealOffer(NetworkId makerNetworkId) {
        return new BisqEasyOffer(makerNetworkId,
                Direction.SELL,
                new Market("BTC", "EUR", "Bitcoin", "Euro"),
                new BaseSideFixedAmountSpec(100_000),
                new MarketPriceSpec(),
                List.of(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                "",
                List.of("en"),
                "1.0.0");
    }

    private static NetworkId createNetworkId(String keyId) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), keyId));
    }
}
