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

import bisq.bonded_roles.release.AppType;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;

import static bisq.trade.bisq_easy.BisqEasyTakeOfferRequestTestFixtures.makerAcceptsTakeOfferRequests;
import static bisq.trade.bisq_easy.BisqEasyTakeOfferRequestTestFixtures.takeOfferRequest;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsTakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.channel;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.inState;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.sellerAsMakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.userIdentity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.userProfile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The trade service fails a trade whose chat channel does not match the trade or its contract as soon as both
 * exist: at startup for stored pairs, when a channel appears for a stored trade, and when a trade is created while
 * its channel exists.
 */
class BisqEasyTradeServiceChatChannelBindingTest {
    private ServiceProvider serviceProvider;
    private BisqEasyTradeService service;
    private ObservableSet<BisqEasyOpenTradeChannel> channels;
    private Identity makerIdentity;
    private Identity takerIdentity;
    private NetworkId makerNetworkId;
    private BisqEasyOffer offer;
    private BisqEasyTrade trade;

    @BeforeEach
    void setUp() {
        serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        BisqEasyOpenTradeChannelService channelService = mock(BisqEasyOpenTradeChannelService.class);
        when(serviceProvider.getChatService().getBisqEasyOpenTradeChannelService()).thenReturn(channelService);
        channels = new ObservableSet<>();
        when(channelService.getChannels()).thenReturn(channels);
        when(channelService.findChannelByTradeId(anyString())).thenAnswer(invocation -> channels.stream()
                .filter(channel -> channel.getTradeId().equals(invocation.getArgument(0)))
                .findAny());
        service = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP);

        makerIdentity = identity("maker");
        makerNetworkId = makerIdentity.getNetworkId();
        offer = offer("offer-id", makerNetworkId, QUOTE_AMOUNT);
        takerIdentity = identity("taker");
        trade = buyerAsTakerTrade(contract(offer, takerIdentity.getNetworkId(), Optional.empty()), takerIdentity);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    private BisqEasyOpenTradeChannel channelWith(BisqEasyOffer channelOffer) {
        return channel(trade, channelOffer, userProfile(makerNetworkId), Optional.empty());
    }

    @Test
    void storedMismatchingPairFailsAtStartup() {
        channels.add(channelWith(offer("offer-id", makerNetworkId, QUOTE_AMOUNT * 2)));
        service.getPersistableStore().addTrade(trade);

        service.initialize();

        assertEquals(BisqEasyTradeState.FAILED, trade.getState());
        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, trade.getTradeProtocolFailure());
    }

    @Test
    void storedMatchingPairIsUntouchedAtStartup() throws UnresolvableProtobufException {
        channels.add(channelWith(BisqEasyOffer.fromProto(offer.toProto(false))));
        service.getPersistableStore().addTrade(trade);

        service.initialize();

        assertEquals(BisqEasyTradeState.INIT, trade.getState());
    }

    @Test
    void completedTradeIsUntouchedAtStartup() {
        BisqEasyTrade completedTrade = inState(trade, BisqEasyTradeState.BTC_CONFIRMED);
        channels.add(channel(completedTrade, offer("offer-id", makerNetworkId, QUOTE_AMOUNT * 2), userProfile(makerNetworkId), Optional.empty()));
        service.getPersistableStore().addTrade(completedTrade);

        service.initialize();

        assertEquals(BisqEasyTradeState.BTC_CONFIRMED, completedTrade.getState());
    }

    @Test
    void mismatchingChannelAppearingForStoredTradeFailsTrade() {
        service.getPersistableStore().addTrade(trade);
        service.initialize();

        channels.add(channelWith(offer("offer-id", makerNetworkId, QUOTE_AMOUNT * 2)));

        assertEquals(BisqEasyTradeState.FAILED, trade.getState());
    }

    @Test
    void matchingChannelAppearingForStoredTradeIsAccepted() {
        service.getPersistableStore().addTrade(trade);
        service.initialize();

        channels.add(channelWith(offer));

        assertEquals(BisqEasyTradeState.INIT, trade.getState());
    }

    @Test
    void makerChannelUnderTheTradeIdentityAppearingForStoredMakerTradeIsAccepted() {
        BisqEasyTrade makerTrade = sellerAsMakerTrade(trade.getContract(), makerIdentity);
        service.getPersistableStore().addTrade(makerTrade);
        service.initialize();

        channels.add(channel(makerTrade, offer, userProfile(takerIdentity.getNetworkId()), Optional.empty()));

        assertEquals(BisqEasyTradeState.INIT, makerTrade.getState());
    }

    @Test
    void makerChannelUnderAnotherLocalIdentityAppearingForStoredMakerTradeFailsTrade() {
        BisqEasyTrade makerTrade = sellerAsMakerTrade(trade.getContract(), makerIdentity);
        service.getPersistableStore().addTrade(makerTrade);
        service.initialize();

        // The taker addressed its first chat message to another identity of the maker.
        channels.add(channel(makerTrade,
                offer,
                userIdentity(identity("other-maker-identity")),
                userProfile(takerIdentity.getNetworkId()),
                Optional.empty()));

        assertEquals(BisqEasyTradeState.FAILED, makerTrade.getState());
        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, makerTrade.getTradeProtocolFailure());
    }

    @Test
    void mismatchingChannelExistingWhenMakerTradeIsCreatedFailsTrade() {
        makerAcceptsTakeOfferRequests(serviceProvider, makerIdentity, offer);
        service.initialize();
        BisqEasyContract contract = trade.getContract();
        // The taker's first chat message arrived before the take offer request and created the maker's channel.
        channels.add(channel(sellerAsMakerTrade(contract, makerIdentity),
                offer("offer-id", makerNetworkId, QUOTE_AMOUNT * 2),
                userProfile(takerIdentity.getNetworkId()),
                Optional.empty()));

        service.onMessage(takeOfferRequest(trade.getId(), takerIdentity, contract));

        BisqEasyTrade makerTrade = service.findTrade(trade.getId()).orElseThrow();
        assertEquals(BisqEasyTradeState.FAILED, makerTrade.getState());
        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, makerTrade.getTradeProtocolFailure());
    }

    @Test
    void chatChannelIsCreatedOnlyForATradeWithThatPeer() {
        BisqEasyOpenTradeChannelService channelService = serviceProvider.getChatService().getBisqEasyOpenTradeChannelService();
        service.getPersistableStore().addTrade(trade);
        service.initialize();

        ArgumentCaptor<BiPredicate<String, String>> isTradeWithPeer = ArgumentCaptor.captor();
        verify(channelService).setIsTradeWithPeer(isTradeWithPeer.capture());
        assertTrue(isTradeWithPeer.getValue().test(trade.getId(), makerNetworkId.getId()));
        assertFalse(isTradeWithPeer.getValue().test(trade.getId(), takerIdentity.getNetworkId().getId()));
        assertFalse(isTradeWithPeer.getValue().test("unknownTradeId", makerNetworkId.getId()));
        verify(channelService).processPendingMessages(trade.getId());
    }

    @Test
    void pendingChatMessagesAreProcessedWhenMakerTradeIsCreated() {
        BisqEasyOpenTradeChannelService channelService = serviceProvider.getChatService().getBisqEasyOpenTradeChannelService();
        makerAcceptsTakeOfferRequests(serviceProvider, makerIdentity, offer);
        service.initialize();
        AtomicBoolean tradeStoredWhenProcessing = new AtomicBoolean();
        doAnswer(invocation -> {
            tradeStoredWhenProcessing.set(service.findTrade(invocation.getArgument(0)).isPresent());
            return null;
        }).when(channelService).processPendingMessages(anyString());

        service.onMessage(takeOfferRequest(trade.getId(), takerIdentity, trade.getContract()));

        verify(channelService).processPendingMessages(trade.getId());
        assertTrue(tradeStoredWhenProcessing.get());
    }

}
