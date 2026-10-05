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

package bisq.chat.bisq_easy.open_trades;

import bisq.chat.ChatMessageType;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The trade channel on the mediator's node is built from the verified mediation request. Anybody who knows the trade
 * ID can send the mediator a chat message with an offer before the request. That creates a channel with the sender
 * as the only trader, in which the messages of the other trader are dropped.
 */
class BisqEasyOpenTradeMediatorChannelTest {
    private static final String REQUESTER_PROFILE_ID = "1111111111111111111111111111111111111111";
    private static final String PEER_PROFILE_ID = "2222222222222222222222222222222222222222";
    private static final String MEDIATOR_PROFILE_ID = "5555555555555555555555555555555555555555";
    private static final String TRADE_ID = "tradeId";

    private BisqEasyOpenTradeChannelService service;
    private UserIdentity mediatorIdentity;
    private UserProfile mediator;
    private UserProfile requester;
    private UserProfile peer;
    private BisqEasyOffer offer;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        mediator = userProfile(MEDIATOR_PROFILE_ID);
        mediatorIdentity = userIdentity(mediator);
        requester = userProfile(REQUESTER_PROFILE_ID);
        peer = userProfile(PEER_PROFILE_ID);
        offer = mock(BisqEasyOffer.class);

        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        when(userIdentityService.findUserIdentity(MEDIATOR_PROFILE_ID)).thenReturn(Optional.of(mediatorIdentity));
        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        service = new BisqEasyOpenTradeChannelService(persistenceService, mock(NetworkService.class), userService);
    }

    // The mediator has no trade, so a chat message creates no channel at the mediator. It waits, and the channel which
    // the request creates takes it.
    @Test
    void chatMessageWithOfferBeforeTheRequestCreatesNoChannel() {
        BisqEasyOpenTradeMessage messageOfPeer = messageToMediator(peer, Optional.of(offer));
        service.onMessage(messageOfPeer);
        assertTrue(service.getChannels().isEmpty());

        BisqEasyOpenTradeChannel channel = service.mediatorFindOrCreatesChannel(TRADE_ID, offer, mediatorIdentity, requester, peer);

        assertEquals(Set.of(REQUESTER_PROFILE_ID, PEER_PROFILE_ID), traderIds(channel));
        assertTrue(channel.getChatMessages().contains(messageOfPeer));
    }

    @Test
    void channelWhichAChatMessageCreatedBeforeTheRequestIsReplaced() {
        channelCreatedByChatMessageOfOlderVersion();
        BisqEasyOpenTradeChannel channelOfChatMessage = service.findChannelByTradeId(TRADE_ID).orElseThrow();

        BisqEasyOpenTradeChannel channel = service.mediatorFindOrCreatesChannel(TRADE_ID, offer, mediatorIdentity, requester, peer);

        assertNotSame(channelOfChatMessage, channel);
        assertSame(channel, service.findChannelByTradeId(TRADE_ID).orElseThrow());
        assertEquals(1, service.getChannels().size());
        assertEquals(Set.of(REQUESTER_PROFILE_ID, PEER_PROFILE_ID), traderIds(channel));
        assertTrue(channel.isMediator());
        assertTrue(channel.isInMediation());
        // The message which created the replaced channel is dropped with it
        assertTrue(channel.getChatMessages().isEmpty());
    }

    @Test
    void messagesOfTheOtherTraderAreAcceptedAfterTheReplacement() {
        channelCreatedByChatMessageOfOlderVersion();
        BisqEasyOpenTradeChannel channel = service.mediatorFindOrCreatesChannel(TRADE_ID, offer, mediatorIdentity, requester, peer);

        BisqEasyOpenTradeMessage messageOfRequester = messageToMediator(requester, Optional.empty());
        service.onMessage(messageOfRequester);

        assertTrue(channel.getChatMessages().contains(messageOfRequester));
    }

    // A message which arrives before the channel exists waits, and the new channel takes it
    @Test
    void messageOfATraderWhichArrivedBeforeTheRequestIsAddedToTheNewChannel() {
        BisqEasyOpenTradeMessage messageOfRequester = messageToMediator(requester, Optional.empty());
        service.onMessage(messageOfRequester);

        BisqEasyOpenTradeChannel channel = service.mediatorFindOrCreatesChannel(TRADE_ID, offer, mediatorIdentity, requester, peer);

        assertTrue(channel.getChatMessages().contains(messageOfRequester));
    }

    // The request of the other trader names the parties the other way round
    @Test
    void mediationChannelIsReused() {
        BisqEasyOpenTradeChannel channel = service.mediatorFindOrCreatesChannel(TRADE_ID, offer, mediatorIdentity, requester, peer);
        BisqEasyOpenTradeMessage messageOfRequester = messageToMediator(requester, Optional.empty());
        service.onMessage(messageOfRequester);

        assertSame(channel, service.mediatorFindOrCreatesChannel(TRADE_ID, offer, mediatorIdentity, peer, requester));
        assertTrue(channel.getChatMessages().contains(messageOfRequester));
    }

    // The mediator does not check the signatures of the contract. A party can send a request with another contract of
    // the same trade ID, which names other parties. Such a request must not replace the channel of the case.
    @Test
    void mediationChannelIsNotReplacedByARequestWithOtherParties() {
        BisqEasyOpenTradeChannel channel = service.mediatorFindOrCreatesChannel(TRADE_ID, offer, mediatorIdentity, requester, peer);

        UserProfile otherProfile = userProfile("3333333333333333333333333333333333333333");
        assertSame(channel, service.mediatorFindOrCreatesChannel(TRADE_ID, offer, mediatorIdentity, peer, otherProfile));
    }

    // The node of the mediator can hold another identity which trades in the trade. A node has one channel per trade,
    // and the channel of the own trade must not be replaced.
    @Test
    void channelOfAnotherOwnIdentityIsKept() {
        UserIdentity requesterIdentity = userIdentity(requester);
        BisqEasyOpenTradeChannel tradersChannel = service.traderFindOrCreatesChannel(TRADE_ID,
                offer,
                requesterIdentity,
                peer,
                Optional.of(mediator));

        assertSame(tradersChannel, service.mediatorFindOrCreatesChannel(TRADE_ID, offer, mediatorIdentity, requester, peer));
    }

    // Older versions created a channel from a chat message with an offer without a trade, and such a channel can be in
    // the persisted store. The predicate which holds for any trade lets the message create it.
    private void channelCreatedByChatMessageOfOlderVersion() {
        service.setIsTradeWithPeer((tradeId, userProfileId) -> true);
        service.onMessage(messageToMediator(peer, Optional.of(offer)));
        service.setIsTradeWithPeer((tradeId, userProfileId) -> false);
    }

    private BisqEasyOpenTradeMessage messageToMediator(UserProfile sender, Optional<BisqEasyOffer> bisqEasyOffer) {
        NetworkId receiverNetworkId = mock(NetworkId.class);
        when(receiverNetworkId.getId()).thenReturn(MEDIATOR_PROFILE_ID);
        return new BisqEasyOpenTradeMessage(TRADE_ID,
                StringUtils.createUid(),
                BisqEasyOpenTradeChannel.createId(TRADE_ID),
                sender,
                MEDIATOR_PROFILE_ID,
                receiverNetworkId,
                "text",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                Optional.of(mediator),
                ChatMessageType.TEXT,
                bisqEasyOffer,
                new HashSet<>());
    }

    private static Set<String> traderIds(BisqEasyOpenTradeChannel channel) {
        return channel.getTraders().stream().map(UserProfile::getId).collect(Collectors.toSet());
    }

    private static UserProfile userProfile(String profileId) {
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.getId()).thenReturn(profileId);
        return userProfile;
    }

    private static UserIdentity userIdentity(UserProfile userProfile) {
        UserIdentity userIdentity = mock(UserIdentity.class);
        String profileId = userProfile.getId();
        when(userIdentity.getId()).thenReturn(profileId);
        when(userIdentity.getUserProfile()).thenReturn(userProfile);
        return userIdentity;
    }
}
