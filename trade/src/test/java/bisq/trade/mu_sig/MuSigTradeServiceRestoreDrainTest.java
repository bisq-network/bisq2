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

package bisq.trade.mu_sig;

import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.release.AppType;
import bisq.common.market.Market;
import bisq.common.network.Address;
import bisq.common.observable.Observable;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufEnumException;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.NetworkMessageResolver;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.messages.network.SendAccountPayloadMessage;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
import bisq.trade.protocol.messages.TradeMessage;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MuSig parity: the generic {@code Trade}/{@code FsmModel}
 * persistence fix (persisted pendingFsmEvents + Fsm#drainEventQueue) applies to every {@code Trade} subclass,
 * including {@link MuSigTrade} - but only {@code BisqEasyTradeService} actually drained restored trades on load.
 * {@link MuSigTradeService} persisted the queue (via the same generic Trade machinery) without ever draining it,
 * so a MuSig trade with an out-of-order message parked at shutdown would stay stuck forever after a restart,
 * even though the data needed to self-heal was sitting right there in the persisted proto.
 * <br/>
 * A full onMessage()/initialize()-driven lifecycle test analogous to
 * {@code BisqEasyTradeTest#confirmFiatSentMessageQueuedWhileBtcAddressPendingSurvivesRestoreAndDrainsViaRealServiceLifecycle}
 * is impractical for MuSig: essentially every message handler in the setup phase (SetupTradeMessage_B/C/D and
 * their handlers) calls out to {@code MusigGrpcClient}'s real blocking gRPC stub (nonce shares, partial
 * signatures, deposit tx signing), which would require standing up (or deeply mocking) a MuSig gRPC server - out
 * of proportion for this regression test. {@link SendAccountPayloadMessage}'s handler is the one early-phase
 * message handler that does NOT touch gRPC (its only side effect,
 * {@code MuSigTradeService#observeDepositTxConfirmationStatus}, is itself a dev-time no-op - see the
 * {@code if (true) return;} guard at the top of that method), which makes a genuine out-of-order-message
 * restore-and-drain scenario reproducible here without any gRPC involvement.
 * <p>
 * This is PR B of a multi-PR split of the original fix - see {@code BisqEasyTradeTest}'s class javadoc for what
 * is intentionally not ported yet (the creation-atomicity/id-derivation test for MuSig's mirror of that guard is
 * excluded here for the same reason).
 */
class MuSigTradeServiceRestoreDrainTest {
    static {
        // Normally registered once at app startup by bisq.application.ResolverConfig, which is not a dependency
        // available to :trade tests. Required so a persisted, wrapped TradeMessage (see Trade#pendingEventsFromProto)
        // can be resolved back via EnvelopePayloadMessage.fromProto() -> Any.unpack() - shared by both BisqEasy and
        // MuSig trade messages (see TradeMessage#fromProto's MessageCase switch).
        NetworkMessageResolver.addResolver("trade.TradeMessage", TradeMessage.getNetworkMessageResolver());
    }

    /**
     * End-to-end reproduction of the MuSig restore-drain gap: a buyer-as-taker trade is hand-placed at
     * {@code TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX} with a {@link SendAccountPayloadMessage} already queued in
     * its persisted {@code pendingFsmEvents} (as if it had arrived out of order and survived a restart via the
     * generic Trade persistence fix, but before this fix was applied to MuSigTradeService). The trade is then
     * registered via {@link MuSigTradeService#createAndAddTradeProtocol(MuSigTrade, boolean)} with
     * {@code isRestoredTrade=true} - the exact call {@code initialize()} now makes for every persisted trade at
     * app startup - and the queued message must be re-applied immediately, without any further live message ever
     * arriving.
     */
    @Test
    void restoredTradeWithQueuedMessageDrainsAndAdvancesOnRestore() throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("musig-buyer-taker");
        NetworkId makerNetworkId = createNetworkId("musig-seller-maker");

        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        MuSigOffer offer = new MuSigOffer(
                "musig-offer-id",
                makerNetworkId,
                Direction.SELL,
                market,
                new BaseSideFixedAmountSpec(100_000),
                new MarketPriceSpec(),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                List.of(),
                "1.0.0");
        MuSigContract contract = new MuSigContract(
                System.currentTimeMillis(),
                offer,
                takerNetworkId,
                100_000,
                3_500_000,
                PaymentMethodSpecUtil.createPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK), "USD"),
                Optional.empty(),
                new MarketPriceSpec(),
                0);

        // Buyer-as-taker trade, hand-placed at the state reached right after publishing the deposit tx - the
        // predecessor state for SendAccountPayloadMessage (see MuSigBuyerAsTakerProtocol#configTransitions) -
        // with the message already parked in the persisted pendingFsmEvents, exactly mirroring
        // BisqEasyTradeTest's out-of-order simulation via toProto()/fromProto().
        MuSigTrade freshTrade = new MuSigTrade(contract, true, true, createIdentity(takerNetworkId), offer, takerNetworkId, makerNetworkId);
        String tradeId = freshTrade.getId();

        SendAccountPayloadMessage sendAccountPayloadMessage = new SendAccountPayloadMessage(
                "send-account-payload-msg", tradeId, MuSigProtocol.VERSION, makerNetworkId, takerNetworkId,
                new UserDefinedFiatAccountPayload("test-id", "test-account-data"));

        bisq.trade.protobuf.Trade proto = freshTrade.toProto(false).toBuilder()
                .setState(MuSigTradeState.TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX.name())
                .addPendingFsmEvents(sendAccountPayloadMessage.toProto(false))
                .build();
        MuSigTrade restoredTrade = MuSigTrade.fromProto(proto);
        assertEquals(1, restoredTrade.getEventQueue().size());
        assertEquals(MuSigTradeState.TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX, restoredTrade.getTradeState());

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        NetworkService networkService = mock(NetworkService.class);
        when(serviceProvider.getNetworkService()).thenReturn(networkService);

        MuSigTradeService tradeService = new MuSigTradeService(
                new MuSigTradeService.Config("localhost", 0), serviceProvider, AppType.DESKTOP);
        when(serviceProvider.getMuSigTradeService()).thenReturn(tradeService);

        // The real, package-private restore-time entry point - mirrors exactly what initialize() now calls for
        // every persisted trade (see MuSigTradeService#initialize(): "persistableStore.getTrades().forEach(trade
        // -> createAndAddTradeProtocol(trade, true))"). isRestoredTrade=true must drain the queue once,
        // immediately, without waiting for some later, unrelated live transition to happen to occur for this trade.
        tradeService.createAndAddTradeProtocol(restoredTrade, true);

        // The load-bearing assertion: on the fix, the parked SendAccountPayloadMessage was re-applied purely by
        // virtue of being restored and registered - the trade advanced from TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX
        // to TAKER_RECEIVED_ACCOUNT_PAYLOAD with no further message ever delivered. Pre-fix (isRestoredTrade drain
        // wiring absent, as MuSigTradeService#initialize() used to call the plain 1-arg createAndAddTradeProtocol),
        // this trade would stay stuck at TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX forever, exactly like #1622 before
        // BisqEasyTradeService got its drain-on-restore fix.
        assertEquals(MuSigTradeState.TAKER_RECEIVED_ACCOUNT_PAYLOAD, restoredTrade.getTradeState());
        assertTrue(restoredTrade.getEventQueue().isEmpty());
    }

    /**
     * Isolation guard mirroring {@code BisqEasyTradeService#createAndAddTradeProtocol}'s per-trade try/catch:
     * a trade restored with a queued event that no longer matches ANY transition in its (possibly since-changed)
     * protocol config must not prevent the trade from being registered - drainEventQueue() re-queues an
     * unmatched event rather than applying it, so this also pins that a mismatched queued event is simply left
     * parked (not lost, not thrown) rather than blocking restore.
     */
    @Test
    void restoredTradeWithQueuedMessageThatStillDoesNotMatchIsLeftParkedWithoutBlockingRestore() throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("musig-buyer-taker-mismatch");
        NetworkId makerNetworkId = createNetworkId("musig-seller-maker-mismatch");

        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        MuSigOffer offer = new MuSigOffer(
                "musig-offer-id-mismatch",
                makerNetworkId,
                Direction.SELL,
                market,
                new BaseSideFixedAmountSpec(100_000),
                new MarketPriceSpec(),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                List.of(),
                "1.0.0");
        MuSigContract contract = new MuSigContract(
                System.currentTimeMillis(),
                offer,
                takerNetworkId,
                100_000,
                3_500_000,
                PaymentMethodSpecUtil.createPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK), "USD"),
                Optional.empty(),
                new MarketPriceSpec(),
                0);

        MuSigTrade freshTrade = new MuSigTrade(contract, true, true, createIdentity(takerNetworkId), offer, takerNetworkId, makerNetworkId);
        String tradeId = freshTrade.getId();

        // SendAccountPayloadMessage only matches a transition from TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX. Parking
        // it while the trade is still at the earlier TAKER_INITIALIZED_TRADE state means it genuinely does not
        // match on restore - drainEventQueue() must re-queue it (a safe no-op), not throw or lose it.
        SendAccountPayloadMessage sendAccountPayloadMessage = new SendAccountPayloadMessage(
                "send-account-payload-msg-mismatch", tradeId, MuSigProtocol.VERSION, makerNetworkId, takerNetworkId,
                new UserDefinedFiatAccountPayload("test-id", "test-account-data"));

        bisq.trade.protobuf.Trade proto = freshTrade.toProto(false).toBuilder()
                .setState(MuSigTradeState.TAKER_INITIALIZED_TRADE.name())
                .addPendingFsmEvents(sendAccountPayloadMessage.toProto(false))
                .build();
        MuSigTrade restoredTrade = MuSigTrade.fromProto(proto);
        assertEquals(1, restoredTrade.getEventQueue().size());

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        NetworkService networkService = mock(NetworkService.class);
        when(serviceProvider.getNetworkService()).thenReturn(networkService);

        MuSigTradeService tradeService = new MuSigTradeService(
                new MuSigTradeService.Config("localhost", 0), serviceProvider, AppType.DESKTOP);
        when(serviceProvider.getMuSigTradeService()).thenReturn(tradeService);

        // Must not throw, and the trade must still get registered/returned even though the drain could not
        // apply anything.
        tradeService.createAndAddTradeProtocol(restoredTrade, true);

        assertEquals(MuSigTradeState.TAKER_INITIALIZED_TRADE, restoredTrade.getTradeState());
        assertEquals(1, restoredTrade.getEventQueue().size());
        assertTrue(tradeService.findProtocol(tradeId).isPresent());
    }

    /**
     * MuSig mirror of {@code BisqEasyTradeTest#restoreDrainDropsQueuedEventsFromNowBannedSender}:
     * the queued message passed onMessage()'s banned-sender check when originally received, but
     * the sender got banned before the restart. The restore drain bypasses onMessage(), so the guard in
     * {@link MuSigTradeService#createAndAddTradeProtocol} must scrub the banned sender's queued events before
     * draining. Same trade shape as {@link #restoredTradeWithQueuedMessageDrainsAndAdvancesOnRestore} - a state
     * that ACCEPTS the queued message - so the state NOT advancing proves the drop, not a vacuous no-op.
     */
    @Test
    void restoreDrainDropsQueuedEventsFromNowBannedSender() throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("musig-buyer-taker-banned");
        NetworkId makerNetworkId = createNetworkId("musig-seller-maker-banned");

        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        MuSigOffer offer = new MuSigOffer(
                "musig-offer-id-banned",
                makerNetworkId,
                Direction.SELL,
                market,
                new BaseSideFixedAmountSpec(100_000),
                new MarketPriceSpec(),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                List.of(),
                "1.0.0");
        MuSigContract contract = new MuSigContract(
                System.currentTimeMillis(),
                offer,
                takerNetworkId,
                100_000,
                3_500_000,
                PaymentMethodSpecUtil.createPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK), "USD"),
                Optional.empty(),
                new MarketPriceSpec(),
                0);

        MuSigTrade freshTrade = new MuSigTrade(contract, true, true, createIdentity(takerNetworkId), offer, takerNetworkId, makerNetworkId);
        String tradeId = freshTrade.getId();

        SendAccountPayloadMessage sendAccountPayloadMessage = new SendAccountPayloadMessage(
                "send-account-payload-msg-banned", tradeId, MuSigProtocol.VERSION, makerNetworkId, takerNetworkId,
                new UserDefinedFiatAccountPayload("test-id", "test-account-data"));

        bisq.trade.protobuf.Trade proto = freshTrade.toProto(false).toBuilder()
                .setState(MuSigTradeState.TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX.name())
                .addPendingFsmEvents(sendAccountPayloadMessage.toProto(false))
                .build();
        MuSigTrade restoredTrade = MuSigTrade.fromProto(proto);
        assertEquals(1, restoredTrade.getEventQueue().size());

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        NetworkService networkService = mock(NetworkService.class);
        when(serviceProvider.getNetworkService()).thenReturn(networkService);
        // The sender of the queued message got banned between original receipt and the restart.
        when(serviceProvider.getUserService().getBannedUserService()
                .isUserProfileBanned(any(NetworkId.class))).thenReturn(true);

        MuSigTradeService tradeService = new MuSigTradeService(
                new MuSigTradeService.Config("localhost", 0), serviceProvider, AppType.DESKTOP);
        when(serviceProvider.getMuSigTradeService()).thenReturn(tradeService);

        tradeService.createAndAddTradeProtocol(restoredTrade, true);

        // Dropped, not applied: pre-guard this exact shape advanced to TAKER_RECEIVED_ACCOUNT_PAYLOAD (see
        // restoredTradeWithQueuedMessageDrainsAndAdvancesOnRestore above).
        assertTrue(restoredTrade.getEventQueue().isEmpty(),
                "queued events from a banned sender must be scrubbed before the restore drain");
        assertEquals(MuSigTradeState.TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX, restoredTrade.getTradeState(),
                "a banned sender's queued message must not be applied");
    }

    /**
     * MuSig mirror of {@code BisqEasyTradeTest#redactionPassClearsPendingEventQueueOfOldTrades}: the
     * data-retention pass must scrub the persisted pending-event queue - which can hold a
     * {@link SendAccountPayloadMessage} with the peer's account payload - once the trade passes the redaction
     * threshold, while a trade inside the threshold keeps its queue.
     */
    @Test
    void redactionPassClearsPendingEventQueueOfOldTrades() throws UnresolvableProtobufEnumException {
        NetworkId takerNetworkId = createNetworkId("musig-buyer-taker-redact");
        NetworkId makerNetworkId = createNetworkId("musig-seller-maker-redact");

        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        MuSigOffer offer = new MuSigOffer(
                "musig-offer-id-redact",
                makerNetworkId,
                Direction.SELL,
                market,
                new BaseSideFixedAmountSpec(100_000),
                new MarketPriceSpec(),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                List.of(),
                "1.0.0");
        // Take-offer date past the not-completed-trades redaction threshold (45-90 days, here 90 with
        // numDays=90) - the trade never reaches a final state, so only this date gates the redaction.
        MuSigContract contract = new MuSigContract(
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(91),
                offer,
                takerNetworkId,
                100_000,
                3_500_000,
                PaymentMethodSpecUtil.createPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK), "USD"),
                Optional.empty(),
                new MarketPriceSpec(),
                0);

        MuSigTrade freshTrade = new MuSigTrade(contract, true, true, createIdentity(takerNetworkId), offer,
                takerNetworkId, makerNetworkId);
        SendAccountPayloadMessage sendAccountPayloadMessage = new SendAccountPayloadMessage(
                "send-account-payload-msg-redact", freshTrade.getId(), MuSigProtocol.VERSION, makerNetworkId,
                takerNetworkId, new UserDefinedFiatAccountPayload("test-id", "test-account-data"));
        bisq.trade.protobuf.Trade proto = freshTrade.toProto(false).toBuilder()
                .setState(MuSigTradeState.TAKER_INITIALIZED_TRADE.name())
                .addPendingFsmEvents(sendAccountPayloadMessage.toProto(false))
                .build();
        MuSigTrade restoredTrade = MuSigTrade.fromProto(proto);
        assertEquals(1, restoredTrade.getEventQueue().size());

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getNetworkService()).thenReturn(mock(NetworkService.class));
        MuSigTradeService tradeService = new MuSigTradeService(
                new MuSigTradeService.Config("localhost", 0), serviceProvider, AppType.DESKTOP);
        when(serviceProvider.getSettingsService().getNumDaysAfterRedactingTradeData())
                .thenReturn(new Observable<>(90));
        tradeService.getPersistableStore().addTrade(restoredTrade);

        tradeService.maybeRedactDataOfCompletedTrades();

        assertTrue(restoredTrade.getEventQueue().isEmpty(),
                "queued events of a trade past the redaction threshold must be scrubbed");
    }

    private static Identity createIdentity(NetworkId networkId) {
        KeyBundle keyBundle = new KeyBundle("test-key-bundle",
                KeyGeneration.generateDefaultEcKeyPair(),
                TorKeyGeneration.generateKeyPair(),
                I2PKeyGeneration.generateKeyPair());
        return new Identity("test-id", networkId, keyBundle);
    }

    private static NetworkId createNetworkId(String keyId) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), keyId));
    }
}
