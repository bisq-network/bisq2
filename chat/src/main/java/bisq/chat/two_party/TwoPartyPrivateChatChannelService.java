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

package bisq.chat.two_party;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.priv.PrivateChatChannelService;
import bisq.chat.reactions.Reaction;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class TwoPartyPrivateChatChannelService extends PrivateChatChannelService<TwoPartyPrivateChatMessageReaction,
        TwoPartyPrivateChatMessage, TwoPartyPrivateChatChannel, TwoPartyPrivateChatChannelStore> {
    @Getter
    private final TwoPartyPrivateChatChannelStore persistableStore = new TwoPartyPrivateChatChannelStore();
    @Getter
    private final Persistence<TwoPartyPrivateChatChannelStore> persistence;

    public TwoPartyPrivateChatChannelService(PersistenceService persistenceService,
                                             NetworkService networkService,
                                             UserService userService,
                                             ChatChannelDomain chatChannelDomain) {
        super(networkService, userService, chatChannelDomain);
        String name = StringUtils.capitalize(StringUtils.snakeCaseToCamelCase(chatChannelDomain.name().toLowerCase()));
        persistence = persistenceService.getOrCreatePersistence(this,
                DbSubDirectory.PRIVATE,
                "Private" + name + "ChatChannelStore",
                persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof TwoPartyPrivateChatMessage) {
            processMessage((TwoPartyPrivateChatMessage) envelopePayloadMessage);
        } else if (envelopePayloadMessage instanceof TwoPartyPrivateChatMessageReaction) {
            processMessageReaction((TwoPartyPrivateChatMessageReaction) envelopePayloadMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableSet<TwoPartyPrivateChatChannel> getChannels() {
        return persistableStore.getChannels();
    }

    public Optional<TwoPartyPrivateChatChannel> findOrCreateChannel(ChatChannelDomain chatChannelDomain,
                                                                    UserProfile peer) {
        synchronized (this) {
            UserIdentity myUserIdentity = userIdentityService.getSelectedUserIdentity();
            return findChannel(chatChannelDomain, peer, myUserIdentity.getId())
                    .or(() -> createAndAddChannel(peer, myUserIdentity.getId()));
        }
    }

    @Override
    public void leaveChannel(TwoPartyPrivateChatChannel channel) {
        if (!channel.getChatMessages().isEmpty()) {
            sendLeaveMessage(channel, channel.getPeer(), new Date().getTime());
        }

        super.leaveChannel(channel);
    }

    public CompletableFuture<SendMessageResult> sendTextMessage(String text,
                                                                Optional<Citation> citation,
                                                                TwoPartyPrivateChatChannel channel) {
        return sendMessage(StringUtils.createUid(),
                text,
                citation,
                channel,
                channel.getPeer(),
                ChatMessageType.TEXT,
                new Date().getTime());
    }

    public CompletableFuture<SendMessageResult> sendTextMessageReaction(TwoPartyPrivateChatMessage message,
                                                                        TwoPartyPrivateChatChannel channel,
                                                                        Reaction reaction,
                                                                        boolean isRemoved) {
        return sendMessageReaction(message, channel, channel.getPeer(), reaction, StringUtils.createUid(), isRemoved);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected TwoPartyPrivateChatMessage createAndGetNewPrivateChatMessage(String messageId,
                                                                           TwoPartyPrivateChatChannel channel,
                                                                           UserProfile senderUserProfile,
                                                                           UserProfile receiverUserProfile,
                                                                           String text,
                                                                           Optional<Citation> citation,
                                                                           long time,
                                                                           boolean wasEdited,
                                                                           ChatMessageType chatMessageType) {
        return new TwoPartyPrivateChatMessage(messageId,
                channel.getChatChannelDomain(),
                channel.getId(),
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                text,
                citation,
                new Date().getTime(),
                wasEdited,
                chatMessageType,
                new HashSet<>());
    }

    @Override
    protected TwoPartyPrivateChatChannel createAndGetNewPrivateChatChannel(UserProfile peer,
                                                                           UserIdentity myUserIdentity) {
        return new TwoPartyPrivateChatChannel(peer, myUserIdentity, chatChannelDomain);
    }

    @Override
    protected TwoPartyPrivateChatMessageReaction createAndGetNewPrivateChatMessageReaction(TwoPartyPrivateChatMessage message,
                                                                                           UserProfile senderUserProfile,
                                                                                           UserProfile receiverUserProfile,
                                                                                           Reaction reaction,
                                                                                           String messageReactionId,
                                                                                           boolean isRemoved) {
        return new TwoPartyPrivateChatMessageReaction(
                messageReactionId,
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                message.getChannelId(),
                message.getChatChannelDomain(),
                message.getId(),
                reaction.ordinal(),
                new Date().getTime(),
                isRemoved
        );
    }

    @Override
    protected Optional<TwoPartyPrivateChatChannel> createNewChannelFromReceivedMessage(TwoPartyPrivateChatMessage message) {
        return createAndAddChannel(message.getSenderUserProfile(), message.getReceiverUserProfileId());
    }

    private Optional<TwoPartyPrivateChatChannel> createAndAddChannel(UserProfile peer, String myUserIdentityId) {
        return userIdentityService.findUserIdentity(myUserIdentityId).map(myUserIdentity -> {
            TwoPartyPrivateChatChannel channel = createAndGetNewPrivateChatChannel(peer, myUserIdentity);
            getChannels().add(channel);
            persist();
            return channel;
        });
    }

    private Optional<TwoPartyPrivateChatChannel> findChannel(ChatChannelDomain chatChannelDomain,
                                                             UserProfile peer,
                                                             String myUserIdentityId) {
        return findChannel(TwoPartyPrivateChatChannel.createId(chatChannelDomain, peer.getId(), myUserIdentityId));
    }
}
