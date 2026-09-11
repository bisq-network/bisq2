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
import bisq.common.market.Market;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufEnumException;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.NetworkMessageResolver;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.persistence.PersistenceService;
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
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatSentMessage;
import bisq.trade.protocol.messages.TradeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Coverage for the stuck-trade fix (#1622): out-of-order protocol messages queued by the FSM survive persistence
 * round-trips and drain on restore (in-memory, via the real service lifecycle and via real disk persistence), and
 * the restore drain honours banned-sender and trading-halt guards.
 * <p>
 * This is PR B of a multi-PR split of the original fix: only the generic {@code trade/} persistence + drain-on-restore
 * plumbing is covered here. Tests exercising the creation-atomicity guard (duplicate take-offer delivery),
 * the startup register-before-replay handoff, the final-state unthrottled-persist follow-up, and the periodic
 * redaction pass's event-queue scrubbing all depend on code that lives in later PRs and are intentionally not
 * ported yet - see the PR B report for the full list.
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
     * public API - {@link BisqEasyTradeService#onMessage(bisq.network.p2p.message.EnvelopePayloadMessage)}, the
     * real {@code ConfidentialMessageService.Listener} entry point for every inbound trade message, and
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
            // JUnit's temp-dir deletion fail (flaky "Failed to close extension context"). Persistence#persistAsync's
            // future queues behind every earlier write on the FIFO executor, so joining it = quiescence. (This
            // bypasses RateLimitedPersistenceClient's throttle directly rather than via a persistNow()-style
            // unthrottled hook, which does not exist yet on this branch - see the PR B report.)
            tradeServiceA.getPersistence().persistAsync(tradeServiceA.getPersistableStore().getClone()).join();
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
            tradeServiceB.getPersistence().persistAsync(tradeServiceB.getPersistableStore().getClone()).join();
            tradeServiceB.shutdown();
        }
    }

    /**
     * Strengthens the real-service-lifecycle exercising the
     * ACTUAL on-disk persistence round trip - {@link BisqEasyTradeService#persist()} (real
     * {@code RateLimitedPersistenceClient#persist()} -> {@code Persistence#persistAsync()/write()}), then
     * {@link BisqEasyTradeService#readPersisted()} (real {@code Persistence#read()}) - rather than a manual
     * {@code trade.toProto()}/{@code BisqEasyTrade.fromProto()} call. The previous version of this test (see
     * above) only proved the FSM/Trade proto (de)serialization survives a round trip, not that the actual
     * persistence write/read path a running node relies on does. This also exercises the atomic
     * {state, eventQueue} snapshot (see {@code FsmModel#getStateAndEventQueueSnapshot} /
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
            // window and be silently dropped without writing anything. Rather than racing that throttle, we go
            // around it via the lower-level Persistence#persistAsync() directly: it runs on the same
            // single-threaded write executor as every persist() call, so waiting for THIS write to complete
            // guarantees any earlier-submitted write (like the automatic one above) has already landed too (FIFO).
            tradeServiceA.getPersistence().persistAsync(tradeServiceA.getPersistableStore().getClone()).join();
        } finally {
            tradeServiceA.getPersistence().persistAsync(tradeServiceA.getPersistableStore().getClone()).join();
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
            tradeServiceB.getPersistence().persistAsync(tradeServiceB.getPersistableStore().getClone()).join();
            tradeServiceB.shutdown();
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
            tradeService.getPersistence().persistAsync(tradeService.getPersistableStore().getClone()).join();
            tradeService.shutdown();
        }
    }

    /**
     * With an EMERGENCY halt-trading alert active (persisted alert data is
     * replayed synchronously when initialize() registers its observer - which happens BEFORE the restore),
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
            haltedTradeService.getPersistence().persistAsync(haltedTradeService.getPersistableStore().getClone()).join();
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
            liftedTradeService.getPersistence().persistAsync(liftedTradeService.getPersistableStore().getClone()).join();
            liftedTradeService.shutdown();
        }
    }

    /**
     * The data-retention pass must scrub the persisted pending-event queue - which can hold account-data
     * messages - once a trade passes the redaction threshold. That includes trades whose payment-account field
     * is empty or already redacted, which a field-only check would skip entirely. Trades still inside the
     * threshold keep their queue untouched.
     */
    @Test
    void redactionPassClearsPendingEventQueueOfOldTrades(@TempDir Path tempDir) throws UnresolvableProtobufEnumException {
        Res.setAndApplyLanguageTag("en");
        long oldTakeOfferDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(91);

        NetworkId takerA = createNetworkId("buyer-taker-redact-a");
        NetworkId makerA = createNetworkId("seller-maker-redact-a");
        BisqEasyOffer offerA = createRealOffer(makerA);
        BisqEasyTrade oldTradeWithAccountData = createRestoredTradeWithQueuedFiatSentMessageAtAcceptingState(
                createRealContract(offerA, takerA, oldTakeOfferDate), offerA, takerA, makerA);
        oldTradeWithAccountData.getPaymentAccountData().set("Sparkasse - IBAN DE02 1234");

        NetworkId takerB = createNetworkId("buyer-taker-redact-b");
        NetworkId makerB = createNetworkId("seller-maker-redact-b");
        BisqEasyOffer offerB = createRealOffer(makerB);
        // Payment-account field never set: the queue is the ONLY place holding sensitive data. The previous
        // field-only filter skipped exactly this trade.
        BisqEasyTrade oldTradeQueueOnly = createRestoredTradeWithQueuedFiatSentMessageAtAcceptingState(
                createRealContract(offerB, takerB, oldTakeOfferDate), offerB, takerB, makerB);

        NetworkId takerC = createNetworkId("buyer-taker-redact-c");
        NetworkId makerC = createNetworkId("seller-maker-redact-c");
        BisqEasyOffer offerC = createRealOffer(makerC);
        BisqEasyTrade recentTrade = createRestoredTradeWithQueuedFiatSentMessageAtAcceptingState(
                createRealContract(offerC, takerC), offerC, takerC, makerC);

        LifecycleHarness harness = createLifecycleHarness(tempDir);
        when(harness.serviceProvider().getSettingsService().getNumDaysAfterRedactingTradeData())
                .thenReturn(new Observable<>(90));
        BisqEasyTradeService tradeService = harness.tradeService();
        try {
            tradeService.getPersistableStore().addTrade(oldTradeWithAccountData);
            tradeService.getPersistableStore().addTrade(oldTradeQueueOnly);
            tradeService.getPersistableStore().addTrade(recentTrade);

            tradeService.maybeRedactDataOfCompletedTrades();

            assertTrue(oldTradeWithAccountData.getEventQueue().isEmpty(),
                    "queued events of a trade past the redaction threshold must be scrubbed");
            assertEquals(Res.get("data.redacted"), oldTradeWithAccountData.getPaymentAccountData().get(),
                    "payment account data past the redaction threshold must be redacted");
            assertTrue(oldTradeQueueOnly.getEventQueue().isEmpty(),
                    "the queue must be scrubbed even when the payment-account field is empty");
            assertEquals(1, recentTrade.getEventQueue().size(),
                    "a trade inside the redaction threshold must keep its queued events");
        } finally {
            tradeService.getPersistence().persistAsync(tradeService.getPersistableStore().getClone()).join();
            tradeService.shutdown();
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
