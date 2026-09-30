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

package bisq.api.web_socket.domain.chat.private_chat;

import bisq.chat.ChatChannelDomain;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.profile.UserProfile;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Test doubles shared by the private-chat WebSocket service tests, all of which push mapped dtos. */
final class PrivateChatTestMocks {
    private PrivateChatTestMocks() {
    }

    /** Exposes whether anything still observes the set, which is what a leaked pin is. */
    static class ObservedSet<T> extends ObservableSet<T> {
        boolean hasObservers() {
            return !observers.isEmpty();
        }
    }

    /**
     * A deep stub is not enough on its own: {@code DtoMappings.UserProfileMapping} runs the profile through
     * Base64 and a digest, and a stubbed {@code byte[]} getter hands back null. The services catch and log
     * the resulting NPE, so without these stubs a mapping failure and a service that decided not to push
     * look exactly the same from the outside.
     */
    static UserProfile mockUserProfile() {
        UserProfile profile = mock(UserProfile.class, RETURNS_DEEP_STUBS);
        when(profile.getNickName()).thenReturn("peer");
        when(profile.getProofOfWork().getPayload()).thenReturn(new byte[0]);
        when(profile.getProofOfWork().getSolution()).thenReturn(new byte[0]);
        when(profile.getNetworkId().getPubKey().getPublicKey().getEncoded()).thenReturn(new byte[0]);
        return profile;
    }

    /**
     * A reaction the dto mapping can process. Everything a deep stub cannot supply: a null enum and a null
     * key both throw inside the mapping, which the services catch and log, so a missing stub here would
     * look exactly like the service deciding not to push.
     */
    static TwoPartyPrivateChatMessageReaction mockReaction(String id,
                                                           UserProfile sender,
                                                           String channelId,
                                                           String messageId,
                                                           boolean isRemoved) {
        TwoPartyPrivateChatMessageReaction reaction =
                mock(TwoPartyPrivateChatMessageReaction.class, RETURNS_DEEP_STUBS);
        when(reaction.getId()).thenReturn(id);
        when(reaction.getSenderUserProfile()).thenReturn(sender);
        when(reaction.getReceiverUserProfileId()).thenReturn("receiver");
        when(reaction.getChatChannelId()).thenReturn(channelId);
        when(reaction.getChatMessageId()).thenReturn(messageId);
        when(reaction.isRemoved()).thenReturn(isRemoved);
        when(reaction.getChatChannelDomain()).thenReturn(ChatChannelDomain.DISCUSSION);
        when(reaction.getReceiverNetworkId().getPubKey().getPublicKey().getEncoded()).thenReturn(new byte[0]);
        return reaction;
    }
}
