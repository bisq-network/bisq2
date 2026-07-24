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
import bisq.common.market.Market;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufEnumException;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the Optional contract of the lifecycle-dependent payment fields (#4719):
 * absent until the respective trade phase sets them, never null.
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
        tradeServiceA.getPersistableStore().addTrade(trade);
        tradeServiceA.initialize();

        BisqEasyConfirmFiatSentMessage fiatSentMessage = new BisqEasyConfirmFiatSentMessage(
                "fiat-sent-msg", tradeId, BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId);
        tradeServiceA.onMessage(fiatSentMessage);

        // Diagnostic only: confirms the message really did get queued rather than applied (pre-existing,
        // unchanged-by-the-fix queuing behavior) - not itself the regression signal.
        assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                trade.getTradeState());

        tradeServiceA.shutdown();

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
            tradeServiceB.shutdown();
        }
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

    private record LifecycleHarness(BisqEasyTradeService tradeService, NetworkService networkService) {
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

        return new LifecycleHarness(tradeService, networkService);
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
        return new BisqEasyContract(System.currentTimeMillis(),
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
