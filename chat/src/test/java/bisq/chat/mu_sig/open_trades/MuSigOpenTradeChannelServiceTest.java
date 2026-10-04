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

package bisq.chat.mu_sig.open_trades;

import bisq.chat.ChatMessageType;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Rules 1 and 5 of docs/specifications/chat/private-channel-membership.md for the MuSig trade chat.
 */
class MuSigOpenTradeChannelServiceTest {
    private static final String MAKER_PROFILE_ID = "1111111111111111111111111111111111111111";
    private static final String TAKER_PROFILE_ID = "2222222222222222222222222222222222222222";
    private static final String THIRD_USER_PROFILE_ID = "3333333333333333333333333333333333333333";
    private static final String TRADE_ID = "tradeId";

    private MuSigOpenTradeChannelService service;
    private UserIdentity makerIdentity;
    private UserProfile taker;
    private UserProfile thirdUser;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(mock(UserIdentityService.class));
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        when(userService.getContactListService()).thenReturn(mock(ContactListService.class));

        makerIdentity = mock(UserIdentity.class);
        when(makerIdentity.getId()).thenReturn(MAKER_PROFILE_ID);
        taker = userProfile(TAKER_PROFILE_ID);
        thirdUser = userProfile(THIRD_USER_PROFILE_ID);

        service = new MuSigOpenTradeChannelService(persistenceService, mock(NetworkService.class), userService);
    }

    @Test
    void messageOfThePeerIsAdded() {
        MuSigOpenTradeChannel channel = createChannel();
        MuSigOpenTradeMessage message = textMessage(taker, TRADE_ID, "message");

        service.onMessage(message);

        assertEquals(Set.of(message), channel.getChatMessages());
    }

    @Test
    void messageOfAUserWhoIsNotAPeerIsNotAdded() {
        MuSigOpenTradeChannel channel = createChannel();

        service.onMessage(textMessage(thirdUser, TRADE_ID, "message"));

        assertTrue(channel.getChatMessages().isEmpty());
    }

    @Test
    void messageWithTheTradeIdOfAnotherTradeIsNotAdded() {
        MuSigOpenTradeChannel channel = createChannel();

        service.onMessage(textMessage(taker, "otherTradeId", "message"));

        assertTrue(channel.getChatMessages().isEmpty());
    }

    @Test
    void pendingMessageOfAUserWhoIsNotAPeerIsNotAddedWhenTheChannelIsCreated() {
        MuSigOpenTradeMessage messageOfPeer = textMessage(taker, TRADE_ID, "messageOfPeer");
        service.onMessage(textMessage(thirdUser, TRADE_ID, "messageOfThirdUser"));
        service.onMessage(messageOfPeer);

        MuSigOpenTradeChannel channel = createChannel();

        assertEquals(Set.of(messageOfPeer), channel.getChatMessages());
    }

    private MuSigOpenTradeChannel createChannel() {
        return service.traderFindOrCreatesChannel(TRADE_ID, makerIdentity, taker, Optional.empty(), Optional.empty());
    }

    private static UserProfile userProfile(String id) {
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.getId()).thenReturn(id);
        return userProfile;
    }

    // A text message to the maker in the channel of TRADE_ID which carries the given trade ID
    private static MuSigOpenTradeMessage textMessage(UserProfile sender, String tradeId, String messageId) {
        return new MuSigOpenTradeMessage(tradeId,
                messageId,
                MuSigOpenTradeChannel.createId(TRADE_ID),
                sender,
                MAKER_PROFILE_ID,
                mock(NetworkId.class),
                "text",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                Optional.empty(),
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());
    }
}
