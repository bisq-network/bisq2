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
import bisq.chat.priv.PendingTradeChatMessages;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyOpenTradePendingMessagesTest {
    private static final String SENDER_PROFILE_ID = "1111111111111111111111111111111111111111";
    private static final String MY_PROFILE_ID = "2222222222222222222222222222222222222222";

    // Anybody can send messages without offer for made-up trade IDs. Such a message never gets a channel.
    @Test
    void pendingMessagesAreBounded() {
        BisqEasyOpenTradeChannelService service = createService();
        UserProfile sender = mock(UserProfile.class);
        when(sender.getId()).thenReturn(SENDER_PROFILE_ID);

        for (int i = 0; i < PendingTradeChatMessages.MAX_PENDING_MESSAGES + 20; i++) {
            service.onMessage(messageWithoutOffer(sender, "tradeId" + i));
        }

        assertEquals(PendingTradeChatMessages.MAX_PENDING_MESSAGES, service.getNumPendingMessages());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BisqEasyOpenTradeChannelService createService() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(BisqEasyOpenTradeChannelStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        // Only a message addressed to one of our identities can become a pending message.
        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        when(userIdentityService.findUserIdentity(anyString())).thenReturn(Optional.empty());
        when(userIdentityService.findUserIdentity(MY_PROFILE_ID)).thenReturn(Optional.of(mock(UserIdentity.class)));
        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        return new BisqEasyOpenTradeChannelService(persistenceService, mock(NetworkService.class), userService);
    }

    private static BisqEasyOpenTradeMessage messageWithoutOffer(UserProfile sender, String tradeId) {
        // The ID of the receiver network ID is the receiver profile ID, as with every unmodified sender
        NetworkId receiverNetworkId = mock(NetworkId.class);
        when(receiverNetworkId.getId()).thenReturn(MY_PROFILE_ID);
        return new BisqEasyOpenTradeMessage(tradeId,
                "messageId-" + tradeId,
                BisqEasyOpenTradeChannel.createId(tradeId),
                sender,
                MY_PROFILE_ID,
                receiverNetworkId,
                "text",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());
    }
}
