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
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.contact_list.ContactListService;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BisqEasyOpenTradeChannelServiceTest {
    private static final String TAKER_PROFILE_ID = "1111111111111111111111111111111111111111";
    private static final String MAKER_PROFILE_ID = "2222222222222222222222222222222222222222";
    private static final String OTHER_PROFILE_ID = "3333333333333333333333333333333333333333";
    private static final String TRADE_ID = "tradeId";

    private BisqEasyOpenTradeChannelService service;
    private UserIdentityService userIdentityService;
    private UserProfile taker;
    private UserIdentity makerIdentity;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        userIdentityService = mock(UserIdentityService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        when(userService.getContactListService()).thenReturn(mock(ContactListService.class));

        makerIdentity = mock(UserIdentity.class);
        when(makerIdentity.getId()).thenReturn(MAKER_PROFILE_ID);
        when(userIdentityService.findUserIdentity(MAKER_PROFILE_ID)).thenReturn(Optional.of(makerIdentity));

        taker = mock(UserProfile.class);
        when(taker.getId()).thenReturn(TAKER_PROFILE_ID);

        service = new BisqEasyOpenTradeChannelService(persistenceService, mock(NetworkService.class), userService);
    }

    @Test
    void takeOfferMessageCreatesChannel() {
        tradeWithTakerExists();
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        BisqEasyOpenTradeMessage message = takeOfferMessage(offer, MAKER_PROFILE_ID, System.currentTimeMillis());

        service.onMessage(message);

        BisqEasyOpenTradeChannel channel = service.findChannelByTradeId(TRADE_ID).orElseThrow();
        assertSame(offer, channel.getBisqEasyOffer());
        assertTrue(channel.getChatMessages().contains(message));
    }

    @Test
    void takeOfferMessageJoinsChannelCreatedByTaker() {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        BisqEasyOpenTradeChannel channel = service.traderFindOrCreatesChannel(TRADE_ID, offer, makerIdentity, taker, Optional.empty());

        BisqEasyOpenTradeMessage message = takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis());
        service.onMessage(message);

        assertEquals(1, service.getChannels().size());
        assertSame(channel, service.findChannelByTradeId(TRADE_ID).orElseThrow());
        assertSame(offer, channel.getBisqEasyOffer());
        assertTrue(channel.getChatMessages().contains(message));
    }

    @Test
    void messageWithTheTradeIdOfAnotherTradeIsNotAddedToTheChannel() {
        BisqEasyOpenTradeChannel channel = service.traderFindOrCreatesChannel(TRADE_ID, mock(BisqEasyOffer.class),
                makerIdentity, taker, Optional.empty());
        BisqEasyOpenTradeMessage messageOfTrade = textMessage(TRADE_ID, "messageOfTrade", MAKER_PROFILE_ID,
                System.currentTimeMillis());
        BisqEasyOpenTradeMessage messageOfAnotherTrade = textMessage("otherTradeId", "messageOfAnotherTrade",
                MAKER_PROFILE_ID, System.currentTimeMillis());

        service.onMessage(messageOfTrade);
        service.onMessage(messageOfAnotherTrade);

        assertEquals(Set.of(messageOfTrade), channel.getChatMessages());
    }

    @Test
    void expiredTakeOfferMessageIsIgnored() {
        long elevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(11);

        service.onMessage(takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, elevenDaysAgo));

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void takeOfferMessageForUnknownReceiverIdentityIsIgnored() {
        service.onMessage(takeOfferMessage(mock(BisqEasyOffer.class), "3333333333333333333333333333333333333333", System.currentTimeMillis()));

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void expiredMessageWithoutOfferIsDroppedAndNotParked() {
        tradeWithTakerExists();
        long elevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(11);
        service.onMessage(textMessage(MAKER_PROFILE_ID, elevenDaysAgo));

        BisqEasyOpenTradeMessage takeOfferMessage = takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis());
        service.onMessage(takeOfferMessage);

        // A parked message would be added to the channel once the take offer message has created it.
        BisqEasyOpenTradeChannel channel = service.findChannelByTradeId(TRADE_ID).orElseThrow();
        assertEquals(1, channel.getChatMessages().size());
        assertTrue(channel.getChatMessages().contains(takeOfferMessage));
    }

    @Test
    void messageWithoutOfferForUnknownReceiverIdentityIsDroppedAndNotParked() {
        tradeWithTakerExists();
        service.onMessage(textMessage("3333333333333333333333333333333333333333", System.currentTimeMillis()));

        BisqEasyOpenTradeMessage takeOfferMessage = takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis());
        service.onMessage(takeOfferMessage);

        BisqEasyOpenTradeChannel channel = service.findChannelByTradeId(TRADE_ID).orElseThrow();
        assertEquals(1, channel.getChatMessages().size());
        assertTrue(channel.getChatMessages().contains(takeOfferMessage));
    }

    // The network layer decrypted the message with the key of its receiver network ID. A message which names another
    // of our identities as receiver must not open a channel of that identity.
    @Test
    void takeOfferMessageWhoseReceiverProfileIdIsNotTheIdOfItsReceiverNetworkIdIsIgnored() {
        tradeWithTakerExists();
        String otherOwnProfileId = "4444444444444444444444444444444444444444";
        when(userIdentityService.findUserIdentity(otherOwnProfileId)).thenReturn(Optional.of(mock(UserIdentity.class)));

        // Such a message fails at parsing (PrivateChatMessage.verify), so it never reaches the service
        assertThrows(IllegalArgumentException.class, () -> takeOfferMessage(mock(BisqEasyOffer.class),
                MAKER_PROFILE_ID,
                networkIdOf(otherOwnProfileId),
                System.currentTimeMillis()));

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void takeOfferMessageWithoutTradeCreatesNoChannel() {
        service.onMessage(takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis()));

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void takeOfferMessageFromAnotherUserThanTheTradePeerCreatesNoChannel() {
        service.setIsTradeWithPeer((tradeId, userProfileId) -> userProfileId.equals(OTHER_PROFILE_ID));

        service.onMessage(takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis()));

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void pendingTakeOfferMessageCreatesChannelWhenTradeExists() {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        BisqEasyOpenTradeMessage message = takeOfferMessage(offer, MAKER_PROFILE_ID, System.currentTimeMillis());
        service.onMessage(message);
        assertTrue(service.getChannels().isEmpty());

        tradeWithTakerExists();
        service.processPendingMessages(TRADE_ID);

        BisqEasyOpenTradeChannel channel = service.findChannelByTradeId(TRADE_ID).orElseThrow();
        assertSame(offer, channel.getBisqEasyOffer());
        assertTrue(channel.getChatMessages().contains(message));
    }

    // The trade service creates the trade and processes the pending messages after the check, before the message is
    // pending. onMessage processes the pending messages again after the message.
    @Test
    void tradeCreatedWhileTheMessageBecomesPendingCreatesChannel() {
        AtomicInteger checks = new AtomicInteger();
        service.setIsTradeWithPeer((tradeId, userProfileId) -> checks.incrementAndGet() > 1);
        BisqEasyOpenTradeMessage message = takeOfferMessage(mock(BisqEasyOffer.class), MAKER_PROFILE_ID, System.currentTimeMillis());

        service.onMessage(message);

        assertTrue(service.findChannelByTradeId(TRADE_ID).orElseThrow().getChatMessages().contains(message));
    }

    private void tradeWithTakerExists() {
        service.setIsTradeWithPeer((tradeId, userProfileId) ->
                tradeId.equals(TRADE_ID) && userProfileId.equals(TAKER_PROFILE_ID));
    }

    private BisqEasyOpenTradeMessage textMessage(String receiverUserProfileId, long date) {
        return textMessage(TRADE_ID, "textMessageId", receiverUserProfileId, date);
    }

    // A text message to the channel of TRADE_ID which carries the given trade ID
    private BisqEasyOpenTradeMessage textMessage(String tradeId,
                                                 String messageId,
                                                 String receiverUserProfileId,
                                                 long date) {
        return new BisqEasyOpenTradeMessage(tradeId,
                messageId,
                BisqEasyOpenTradeChannel.createId(TRADE_ID),
                taker,
                receiverUserProfileId,
                networkIdOf(receiverUserProfileId),
                "text",
                Optional.empty(),
                date,
                false,
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());
    }

    private BisqEasyOpenTradeMessage takeOfferMessage(BisqEasyOffer bisqEasyOffer, String receiverUserProfileId, long date) {
        return takeOfferMessage(bisqEasyOffer, receiverUserProfileId, networkIdOf(receiverUserProfileId), date);
    }

    private BisqEasyOpenTradeMessage takeOfferMessage(BisqEasyOffer bisqEasyOffer,
                                                      String receiverUserProfileId,
                                                      NetworkId receiverNetworkId,
                                                      long date) {
        return new BisqEasyOpenTradeMessage(TRADE_ID,
                "messageId",
                BisqEasyOpenTradeChannel.createId(TRADE_ID),
                taker,
                receiverUserProfileId,
                receiverNetworkId,
                null,
                Optional.empty(),
                date,
                false,
                Optional.of(mock(UserProfile.class)),
                ChatMessageType.TAKE_BISQ_EASY_OFFER,
                Optional.of(bisqEasyOffer),
                new HashSet<>());
    }

    // The ID of a network ID is the hash of its public key, which is also the profile ID of that key
    private static NetworkId networkIdOf(String userProfileId) {
        NetworkId networkId = mock(NetworkId.class);
        when(networkId.getId()).thenReturn(userProfileId);
        return networkId;
    }
}
