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
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Baseline reproduction of the stuck-trade defect discussed in bisq-network/bisq-mobile#1622 / #4885 / #4931.
 * <p>
 * These tests are written EXCLUSIVELY against APIs that exist on {@code main}. Two of them assert the behavior
 * a trader is entitled to expect ("the trade advances once every protocol message has been delivered exactly
 * once") and FAIL on main - each failure is one concrete, deterministic way a Bisq Easy trade gets permanently
 * stuck. The control test passes on main and pins the one recovery path the unchanged FSM does provide, so the
 * failing tests cannot be dismissed as a broken harness.
 * <p>
 * Scenario shared by all three tests (seller-as-maker, the reporter's role in #1622): the trade sits at
 * {@code MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS} - the
 * seller has sent account data, the buyer's btc-address message is the one leg still in flight. The buyer
 * (who by protocol config can only confirm fiat-sent after receiving the seller's account data, see
 * {@code BisqEasyBuyerAsTakerProtocol}) confirms fiat-sent; over Tor/mailbox the two buyer messages can reach
 * the seller in either order.
 * <ol>
 *   <li>{@link #control_outOfOrderFiatSentRecoversWhenEnablingMessageStillArrivesLive}: both messages reach
 *       the running FSM, in the wrong order. The FSM parks the early one and re-applies it after the next
 *       transition. PASSES on main - this is the (correct) "ordinary out-of-order delivery recovers" case.</li>
 *   <li>{@link #tradeAdvancesWhenEnablingMessageCompletesProcessingDuringStartupHandoffWindow}: the
 *       btc-address message finishes network processing inside the startup handoff window - after
 *       {@code initialize()}'s replay snapshot was taken, before {@code addConfidentialMessageListener} runs
 *       (see the ordering in {@code BisqEasyTradeService#initialize()}). The network layer counts it as
 *       delivered (mailbox copy removed, sender resends stopped, re-deliveries deduped), so it is never
 *       delivered again in this session; the fiat-sent message then parks forever. FAILS on main: the trade
 *       is stuck while the app keeps running - no restart involved anywhere.</li>
 *   <li>{@link #parkedOutOfOrderMessageSurvivesRestartSoTradeCompletesWhenEnablingMessageArrives}: the parked
 *       fiat-sent message is wiped by the persistence round trip (the FSM event queue is not serialized on
 *       main), so after a restart the trade advances to the both-legs-done state and sticks one step short,
 *       with the peer's resend machinery long satisfied. FAILS on main: this is why "just restart" is a coin
 *       flip, not a fix.</li>
 * </ol>
 * The harness (real offer/contract/identity/trade, real seller-as-maker protocol and FSM, real
 * {@code BisqEasyTradeService.initialize()}/{@code onMessage()} lifecycle against a mocked network boundary,
 * real proto (de)serialization) mirrors the regression tests shipped with the fix branch, so the same file runs
 * green there unmodified.
 */
class BisqEasyTradeStuckByOutOfOrderMessageProofTest {
    static {
        // Normally registered once at app startup by bisq.application.ResolverConfig, which is not a dependency
        // available to :trade tests. On main this is inert for these tests; on the fix branch it lets the
        // persisted pending events resolve back via EnvelopePayloadMessage.fromProto() -> Any.unpack().
        NetworkMessageResolver.addResolver("trade.TradeMessage", TradeMessage.getNetworkMessageResolver());
    }

    private static final String BTC_ADDRESS = "bc1qxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzx";

    /**
     * Control (PASSES on main): the recovery path the unchanged FSM genuinely provides. Both buyer messages
     * reach the running service, out of order. The early fiat-sent parks; the btc-address transition then
     * auto-drains the queue and the trade completes the fiat-sent step. This pins two things: the harness
     * delivers messages correctly end-to-end, and "ordinary out-of-order delivery recovers" is conceded -
     * the two failing tests below are exactly the cases where its precondition (a later live transition)
     * never materializes.
     */
    @Test
    void control_outOfOrderFiatSentRecoversWhenEnablingMessageStillArrivesLive(@TempDir Path tempDir)
            throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-control");
        NetworkId makerNetworkId = createNetworkId("seller-maker-control");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);
        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        String tradeId = trade.getId();

        LifecycleHarness harness = createLifecycleHarness(tempDir.resolve("control"));
        BisqEasyTradeService tradeService = harness.tradeService();
        tradeService.getPersistableStore().addTrade(trade);
        try {
            tradeService.initialize();

            // Out of order: fiat-sent first. No transition matches -> the FSM parks it.
            tradeService.onMessage(new BisqEasyConfirmFiatSentMessage(
                    "fiat-sent-msg-control", tradeId, BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId));
            assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                    trade.getTradeState(),
                    "the early fiat-sent message must be parked, not applied");
            assertEquals(1, trade.getEventQueue().size(),
                    "the early fiat-sent message must sit in the FSM event queue");
            assertTrue(trade.getEventQueue().stream().anyMatch(BisqEasyConfirmFiatSentMessage.class::isInstance),
                    "the parked event must be the fiat-sent message itself");

            // The enabling message still arrives while the app runs: transition + auto-drain -> recovered.
            tradeService.onMessage(new BisqEasyBtcAddressMessage(
                    "btc-address-msg-control", tradeId, BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId,
                    BTC_ADDRESS, offer));
            assertEquals(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, trade.getTradeState(),
                    "ordinary out-of-order delivery must recover once the enabling transition happens");
        } finally {
            tradeService.persist().join();
            tradeService.shutdown();
        }
    }

    /**
     * FAILS on main - the live stranding, no restart anywhere. The btc-address message finishes network
     * processing inside {@code initialize()}'s handoff window: {@code BisqEasyTradeService#initialize()} first
     * snapshots {@code getProcessedEnvelopePayloadMessages()} for replay and only AFTERWARDS registers itself
     * via {@code addConfidentialMessageListener}. A message whose processing completes between those two steps
     * is dispatched to the listeners registered at that instant ({@code ConfidentialMessageService}
     * notifies only current listeners, then never again for that message) - i.e. to nobody. Nothing brings it
     * back: the local mailbox copy is removed on successful decrypt regardless of listener count; the sender's
     * resend machinery was already satisfied at send time ({@code ResendMessageService} stops resends on
     * {@code ADDED_TO_MAILBOX}, independent of anything the receiving node does); and even a hypothetical
     * re-delivery of the same logical message is swallowed by {@code ConfidentialMessageService}'s
     * decrypted-content dedupe without ever being re-dispatched to listeners.
     * <p>
     * The buyer's fiat-sent then arrives perfectly normally, live - and parks forever: the queue is only
     * drained after a later successful transition, and the only event that could cause one was just consumed
     * unseen. Both protocol messages were delivered by the network exactly once, nothing crashed, the app
     * keeps running - and the trade is stuck. This is the mechanism whose symptom matches the #1622 report
     * ("Mark Payment Sent"/"Payment Received" not advancing while chat still works).
     */
    @Test
    @Timeout(60)
    @Disabled("Startup handoff window loss (#4947) is a separate mechanism this PR does not address: the message is "
            + "consumed with no listener registered, so it never reaches the (now persisted) event queue. The follow-up "
            + "PR fixing #4947 must remove this annotation; the test passes once initialize() no longer drops it.")
    void tradeAdvancesWhenEnablingMessageCompletesProcessingDuringStartupHandoffWindow(@TempDir Path tempDir)
            throws UnresolvableProtobufEnumException {
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
                BTC_ADDRESS, offer);

        AtomicReference<ConfidentialMessageService.Listener> registeredListener = new AtomicReference<>();
        doAnswer(invocation -> {
            registeredListener.set(invocation.getArgument(0));
            return null;
        }).when(networkService).addConfidentialMessageListener(any());

        // Models the network finishing the btc-address message's processing at the point of the handoff
        // window: the replay snapshot (returned set) does not contain it, and dispatch happens to whichever
        // listeners are registered at that instant - exactly ConfidentialMessageService's contract
        // (snapshot read + listeners.forEach on completion; a message is dispatched once, to current
        // listeners only). Whether the trade service sees the message is decided purely by initialize()'s
        // ordering of replay vs. registration - which is the code under test.
        ConfidentialMessageService confidentialMessageService = mock(ConfidentialMessageService.class);
        when(confidentialMessageService.getProcessedEnvelopePayloadMessages()).thenAnswer(invocation -> {
            ConfidentialMessageService.Listener listener = registeredListener.get();
            if (listener != null) {
                listener.onMessage(btcAddressMessage);
            }
            return Set.<EnvelopePayloadMessage>of();
        });
        when(networkService.getConfidentialMessageServices()).thenReturn(Set.of(confidentialMessageService));

        tradeService.getPersistableStore().addTrade(trade);
        try {
            tradeService.initialize();

            // Wiring guard: initialize() must end with the trade service registered as the live listener. If
            // BisqEasyTradeService.initialize()'s replay/registration order is ever restructured, this pins
            // that the mock's model of that ordering still matches the code under test.
            assertSame(tradeService, registeredListener.get(),
                    "initialize() must register the trade service as the confidential message listener");

            // The buyer's fiat-sent arrives live, entirely normally. On main the enabling btc-address message
            // was consumed unseen above, so this parks - and nothing will ever drain it.
            tradeService.onMessage(new BisqEasyConfirmFiatSentMessage(
                    "fiat-sent-msg-handoff", tradeId, BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId));

            assertEquals(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, trade.getTradeState(),
                    "STUCK TRADE: every protocol message was delivered by the network exactly once (btc-address "
                            + "during the startup handoff window - mailbox-cleared, sender resends long since stopped, "
                            + "dispatched to no listener; "
                            + "fiat-sent live, parked in the FSM queue), the app never stopped running, and the seller "
                            + "is now stranded at " + trade.getTradeState()
                            + " with a parked event queue of size " + trade.getEventQueue().size()
                            + " that no future transition will ever drain");
        } finally {
            tradeService.persist().join();
            tradeService.shutdown();
        }
    }

    /**
     * FAILS on main - the restart coin flip. Same starting point; the fiat-sent message arrives early and is
     * parked (in-memory only - while at the network layer the message already counts as delivered: the local
     * mailbox copy is gone and the sender's resends stopped at {@code ADDED_TO_MAILBOX}, so it will never be
     * transmitted again). The node
     * then restarts: the trade is round-tripped through {@code toProto()}/{@code fromProto()} exactly as the
     * persistence layer does. On main the event queue is not serialized, so the parked message is silently
     * wiped. When the genuinely pending btc-address message arrives after the restart, the trade advances to
     * the both-legs-done state - and sticks there, one step short, forever: the fiat-sent confirmation exists
     * nowhere anymore (not in the FSM, not in the mailbox, not in the peer's resend queue).
     * <p>
     * Together with the control test this also explains the #1622 field observation that a desktop restart
     * only sometimes "fixed" the trade: recovery depended on which messages happened to still be around to
     * re-feed, while the FSM's own parked copy was guaranteed to be lost.
     */
    @Test
    void parkedOutOfOrderMessageSurvivesRestartSoTradeCompletesWhenEnablingMessageArrives()
            throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-restart");
        NetworkId makerNetworkId = createNetworkId("seller-maker-restart");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);
        ServiceProvider serviceProvider = createServiceProvider();

        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        String tradeId = trade.getId();
        BisqEasySellerAsMakerProtocol protocol = new BisqEasySellerAsMakerProtocol(serviceProvider, trade);

        // The buyer's fiat-sent arrives before the btc-address leg -> parked (while at the network layer the
        // message already counts as delivered, final - it will never be transmitted again).
        protocol.handle(new BisqEasyConfirmFiatSentMessage(
                "fiat-sent-msg-restart", tradeId, protocol.getVersion(), takerNetworkId, makerNetworkId));
        assertEquals(1, trade.getEventQueue().size(),
                "the early fiat-sent message must be parked in the FSM event queue");
        assertTrue(trade.getEventQueue().stream().anyMatch(BisqEasyConfirmFiatSentMessage.class::isInstance),
                "the parked event must be the fiat-sent message itself");

        // Restart: the exact proto round trip the persistence layer performs. The queue size right after
        // restore is the root-cause checkpoint (0 on main - wiped; the fix preserves it) and is reported in
        // the final assertion, which is branch-neutral.
        bisq.trade.protobuf.Trade proto = trade.toProto(false);
        BisqEasyTrade restoredTrade = BisqEasyTrade.fromProto(proto);
        int queueSizeAfterRestore = restoredTrade.getEventQueue().size();
        BisqEasySellerAsMakerProtocol restoredProtocol = new BisqEasySellerAsMakerProtocol(serviceProvider, restoredTrade);

        // After the restart the genuinely pending btc-address message arrives (it was still unACKed /
        // resend-covered - unlike the fiat-sent one).
        restoredProtocol.handle(new BisqEasyBtcAddressMessage(
                "btc-address-msg-restart", tradeId, restoredProtocol.getVersion(), takerNetworkId, makerNetworkId,
                BTC_ADDRESS, offer));

        assertEquals(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, restoredTrade.getTradeState(),
                "STUCK TRADE: the parked fiat-sent confirmation was wiped by the persistence round trip "
                        + "(event queue size after restore: " + queueSizeAfterRestore + "), so the "
                        + "restored trade strands one step short at " + restoredTrade.getTradeState()
                        + " - and the network will never transmit that message again (mailbox cleared, "
                        + "sender resends stopped at ADDED_TO_MAILBOX, re-deliveries deduped)");
    }


    /* --------------------------------------------------------------------- */
    // Harness - mirrors the fix branch's regression-test scaffolding, restricted to main APIs.
    /* --------------------------------------------------------------------- */

    private record LifecycleHarness(BisqEasyTradeService tradeService, NetworkService networkService,
                                    ServiceProvider serviceProvider) {
    }

    // Real BisqEasyTradeService against real persistence, with every ServiceProvider dependency initialize()
    // itself touches stubbed at the network/bonded-roles boundary.
    private static LifecycleHarness createLifecycleHarness(Path persistenceDir) {
        NetworkService networkService = mock(NetworkService.class);
        // Empty by default: message delivery is driven explicitly in each test body.
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

    // Hand-places a real trade at the given state via the same proto round trip persistence uses. The trade ID
    // is derived (Trade#createId), not chosen - messages must be addressed to the real ID or the handlers'
    // verification rejects them.
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
