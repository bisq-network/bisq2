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
import bisq.contract.ContractSignatureData;
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
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferResponse;
import bisq.trade.protocol.messages.TradeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
     * Live-recovery-pass counterpart to {@link #confirmFiatSentMessageQueuedWhileBtcAddressPendingSurvivesRestoreAndDrains()}:
     * reproduces the same #1622 scenario, but recovers the trade WITHOUT a restart and without the peer re-sending
     * anything, by calling {@link BisqEasyTradeService#reprocessTrade(BisqEasyTrade)} directly. The enabling
     * btc-address message is only ever visible via the confidential-message layer's own live record of processed
     * messages (as it would be if it was received but, for whatever reason, never actually reached the FSM) - it
     * is never delivered to the trade a second time by a peer, and no proto round-trip happens here at all.
     */
    @Test
    void recoveryPassAppliesPendingEnablingMessageAndDrainsQueuedMessageWithoutRestart(@TempDir Path tempDir) throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-recovery");
        NetworkId makerNetworkId = createNetworkId("seller-maker-recovery");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId,
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        String tradeId = trade.getId();

        ConfidentialMessageService confidentialMessageService = mock(ConfidentialMessageService.class);
        RecoveryHarness harness = createRecoveryHarness(tempDir, confidentialMessageService);
        BisqEasyTradeService tradeService = harness.tradeService();

        tradeService.getPersistableStore().addTrade(trade);
        BisqEasyProtocol protocol = tradeService.createAndAddTradeProtocol(trade, false);
        String protocolVersion = protocol.getVersion();

        // The buyer's confirm-fiat-sent message arrives out of order and is parked in the event queue, exactly as
        // a genuine live inbound message would be.
        BisqEasyConfirmFiatSentMessage fiatSentMessage = new BisqEasyConfirmFiatSentMessage(
                "fiat-sent-msg", tradeId, protocolVersion, takerNetworkId, makerNetworkId);
        protocol.handle(fiatSentMessage);
        assertEquals(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                trade.getTradeState());
        assertEquals(1, trade.getEventQueue().size());

        // The genuinely pending btc-address message was received by the confidential-message layer, but never
        // reached the FSM. It is visible only via the network layer's own live record of everything decrypted
        // this session - nothing is redelivered by the peer.
        BisqEasyBtcAddressMessage btcAddressMessage = new BisqEasyBtcAddressMessage(
                "btc-address-msg", tradeId, protocolVersion, takerNetworkId, makerNetworkId,
                "bc1qxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzxyzx", offer);
        Set<EnvelopePayloadMessage> processedMessages = Set.of(btcAddressMessage);
        when(confidentialMessageService.getProcessedEnvelopePayloadMessages()).thenReturn(processedMessages);

        int numApplied = tradeService.reprocessTrade(trade);

        assertEquals(1, numApplied);
        assertEquals(BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, trade.getTradeState());
        assertTrue(trade.getEventQueue().isEmpty());
    }

    /**
     * A healthy, in-progress trade has nothing pending: none of its already-received messages are missing from
     * processedEvents, so the recovery pass must be a complete no-op - no handler re-runs, no message gets sent to
     * the peer, and the trade's state is untouched.
     */
    @Test
    void recoveryPassIsNoOpOnHealthyTradeAndSendsNoMessages(@TempDir Path tempDir) throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-healthy");
        NetworkId makerNetworkId = createNetworkId("seller-maker-healthy");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        BisqEasyTradeState state = BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
        BisqEasyTrade trade = createTradeAtState(contract, offer, takerNetworkId, makerNetworkId, state);

        ConfidentialMessageService confidentialMessageService = mock(ConfidentialMessageService.class);
        // Nothing pending: an empty processed-message set represents "no message this trade needs is sitting
        // anywhere unapplied" - the healthy-trade case the recovery pass must leave completely alone.
        when(confidentialMessageService.getProcessedEnvelopePayloadMessages()).thenReturn(Set.of());
        RecoveryHarness harness = createRecoveryHarness(tempDir, confidentialMessageService);
        BisqEasyTradeService tradeService = harness.tradeService();

        tradeService.getPersistableStore().addTrade(trade);
        tradeService.createAndAddTradeProtocol(trade, false);

        int numApplied = tradeService.reprocessTrade(trade);

        assertEquals(0, numApplied);
        assertEquals(state, trade.getTradeState());
        assertTrue(trade.getEventQueue().isEmpty());
        verify(harness.networkService(), never()).confidentialSend(any(), any(), any());
    }

    /**
     * Guards against the makerCreatesProtocol() IllegalArgumentException regression identified during the #1622
     * recovery-pass safety review: a maker-side trade's own originating take-offer-request/response are always
     * present in the confidential-message layer's processed-message set, since that layer never scopes its record
     * to "not yet applied" - it is a live record of everything decrypted this session. The trade's processedEvents
     * (restored here via the same proto field the persistence fix introduced, mirroring exactly what a real prior
     * transition would have recorded) must exclude both messages from re-selection, proving the recovery pass
     * never routes back through onMessage()/makerCreatesProtocol() - which would otherwise throw, since the
     * protocol/trade already exists for any trade reaching reprocessTrade.
     */
    @Test
    void recoveryPassNeverThrowsAndSkipsAlreadyAppliedTakeOfferMessagesOnMakerTrade(@TempDir Path tempDir) throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("buyer-taker-maker-guard");
        NetworkId makerNetworkId = createNetworkId("seller-maker-guard");
        BisqEasyOffer offer = createRealOffer(makerNetworkId);
        BisqEasyContract contract = createRealContract(offer, takerNetworkId);

        BisqEasyTradeState state = BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
        BisqEasyTrade freshTrade = new BisqEasyTrade(contract, false, false, createIdentity(makerNetworkId), offer,
                takerNetworkId, makerNetworkId);
        bisq.trade.protobuf.Trade proto = freshTrade.toProto(false).toBuilder()
                .setState(state.name())
                // Mirrors Trade#getTradeBuilder: exactly what a real, already-fired take-offer-request/response
                // transition would have recorded before this trade was persisted.
                .addProcessedEventClasses(BisqEasyTakeOfferRequest.class.getName())
                .addProcessedEventClasses(BisqEasyTakeOfferResponse.class.getName())
                .build();
        BisqEasyTrade trade = BisqEasyTrade.fromProto(proto);
        String tradeId = trade.getId();
        assertTrue(trade.getProcessedEvents().contains(BisqEasyTakeOfferRequest.class));
        assertTrue(trade.getProcessedEvents().contains(BisqEasyTakeOfferResponse.class));

        ContractSignatureData contractSignatureData = new ContractSignatureData(new byte[20], new byte[68],
                KeyGeneration.generateDefaultEcKeyPair().getPublic());
        BisqEasyTakeOfferRequest takeOfferRequest = new BisqEasyTakeOfferRequest("take-offer-request-msg", tradeId,
                BisqEasyProtocol.VERSION, takerNetworkId, makerNetworkId, contract, contractSignatureData);
        BisqEasyTakeOfferResponse takeOfferResponse = new BisqEasyTakeOfferResponse("take-offer-response-msg", tradeId,
                BisqEasyProtocol.VERSION, makerNetworkId, takerNetworkId, contractSignatureData);

        ConfidentialMessageService confidentialMessageService = mock(ConfidentialMessageService.class);
        Set<EnvelopePayloadMessage> processedMessages = Set.of(takeOfferRequest, takeOfferResponse);
        when(confidentialMessageService.getProcessedEnvelopePayloadMessages()).thenReturn(processedMessages);
        RecoveryHarness harness = createRecoveryHarness(tempDir, confidentialMessageService);
        BisqEasyTradeService tradeService = harness.tradeService();

        tradeService.getPersistableStore().addTrade(trade);
        tradeService.createAndAddTradeProtocol(trade, false);

        int numApplied = assertDoesNotThrow(() -> tradeService.reprocessTrade(trade));

        assertEquals(0, numApplied);
        assertEquals(state, trade.getTradeState());
        // Never re-created via makerCreatesProtocol(): still exactly one registered trade for this ID.
        assertEquals(1, tradeService.getTrades().stream().filter(t -> t.getId().equals(tradeId)).count());
        // Never re-sent the take-offer-response (or anything else) to the peer.
        verify(harness.networkService(), never()).confidentialSend(any(), any(), any());
    }

    private record RecoveryHarness(BisqEasyTradeService tradeService, NetworkService networkService) {
    }

    private static RecoveryHarness createRecoveryHarness(Path tempDir, ConfidentialMessageService confidentialMessageService) {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getConfidentialMessageServices()).thenReturn(Set.of(confidentialMessageService));
        when(networkService.confidentialSend(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendMessageResult.class)));

        // Deep stubs for everything else: reprocessTrade() and the FSM handlers it may drive only ever touch
        // networkService and (indirectly, via BisqEasyProtocol#persist) the trade service's own persistence - any
        // other ServiceProvider dependency an incidentally-triggered handler reaches for should behave as an inert
        // default (false/empty/no-op) rather than NPE.
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getNetworkService()).thenReturn(networkService);
        when(serviceProvider.getPersistenceService()).thenReturn(new PersistenceService(tempDir));

        BisqEasyTradeService tradeService = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP);
        when(serviceProvider.getBisqEasyTradeService()).thenReturn(tradeService);

        return new RecoveryHarness(tradeService, networkService);
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
