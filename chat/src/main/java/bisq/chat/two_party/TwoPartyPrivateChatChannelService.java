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
import bisq.common.observable.collection.ObservableArray;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class TwoPartyPrivateChatChannelService extends PrivateChatChannelService<TwoPartyPrivateChatMessage, TwoPartyPrivateChatChannel, TwoPartyPrivateChatChannelStore> {
    @Getter
    private final TwoPartyPrivateChatChannelStore persistableStore = new TwoPartyPrivateChatChannelStore();
    @Getter
    private final Persistence<TwoPartyPrivateChatChannelStore> persistence;

    public TwoPartyPrivateChatChannelService(PersistenceService persistenceService,
                                             NetworkService networkService,
                                             UserService userService,
                                             ProofOfWorkService proofOfWorkService,
                                             ChatChannelDomain chatChannelDomain) {
        super(networkService, userService, proofOfWorkService, chatChannelDomain);
        String name = StringUtils.capitalize(StringUtils.snakeCaseToCamelCase(chatChannelDomain.name().toLowerCase()));
        persistence = persistenceService.getOrCreatePersistence(this,
                "db",
                "Private" + name + "ChatChannelStore",
                persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof TwoPartyPrivateChatMessage) {
            processMessage((TwoPartyPrivateChatMessage) networkMessage);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableArray<TwoPartyPrivateChatChannel> getChannels() {
        return persistableStore.getChannels();
    }

    public Optional<TwoPartyPrivateChatChannel> findOrCreateChannel(ChatChannelDomain chatChannelDomain, UserProfile peer) {
        synchronized (this) {
            return Optional.ofNullable(userIdentityService.getSelectedUserIdentity())
                    .flatMap(myUserIdentity -> findChannel(chatChannelDomain, peer, myUserIdentity.getId())
                            .or(() -> createAndAddChannel(peer, myUserIdentity.getId())));
        }
    }

    @Override
    public void leaveChannel(TwoPartyPrivateChatChannel channel) {
        if (channel.isParticipant(channel.getPeer())) {
            sendLeaveMessage(channel, channel.getPeer(), new Date().getTime());
        }

        super.leaveChannel(channel);
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendTextMessage(String text,
                                                                               Optional<Citation> citation,
                                                                               TwoPartyPrivateChatChannel channel) {
        return sendMessage(StringUtils.createShortUid(),
                text,
                citation,
                channel,
                channel.getPeer(),
                ChatMessageType.TEXT,
                new Date().getTime());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected TwoPartyPrivateChatMessage createAndGetNewPrivateChatMessage(String messageId,
                                                                           TwoPartyPrivateChatChannel channel,
                                                                           UserProfile sender,
                                                                           String receiverUserProfileId,
                                                                           String text,
                                                                           Optional<Citation> citation,
                                                                           long time,
                                                                           boolean wasEdited,
                                                                           ChatMessageType chatMessageType) {
        return new TwoPartyPrivateChatMessage(messageId,
                channel.getChatChannelDomain(),
                channel.getId(),
                sender,
                receiverUserProfileId,
                text,
                citation,
                new Date().getTime(),
                wasEdited,
                chatMessageType);
    }

    @Override
    protected TwoPartyPrivateChatChannel createAndGetNewPrivateChatChannel(UserProfile peer, UserIdentity myUserIdentity) {
        return new TwoPartyPrivateChatChannel(peer, myUserIdentity, chatChannelDomain);
    }

    @Override
    protected void processMessage(TwoPartyPrivateChatMessage message) {
        if (canHandleChannelDomain(message) && isValid(message)) {
            findChannel(message)
                    .or(() -> {
                        // We prevent to send leave messages after a peer has left, but there might be still 
                        // race conditions where that might happen, so we check at receiving the message as well, so that
                        // in cases we would get a leave message as first message (e.g. after having closed the channel) 
                        //  we do not create a channel.
                        if (message.getChatMessageType() == ChatMessageType.LEAVE) {
                            log.warn("We received a leave message as first message. This is not expected but might " +
                                    "happen in some rare cases.");
                            return Optional.empty();
                        } else {
                            return createAndAddChannel(message.getSender(), message.getReceiverUserProfileId());
                        }
                    })
                    .ifPresent(channel -> addMessage(message, channel));
        }
    }

    private Optional<TwoPartyPrivateChatChannel> createAndAddChannel(UserProfile peer, String myUserIdentityId) {
        return userIdentityService.findUserIdentity(myUserIdentityId)
                .map(myUserIdentity -> {
                            TwoPartyPrivateChatChannel channel = createAndGetNewPrivateChatChannel(peer, myUserIdentity);
                            getChannels().add(channel);
                            persist();
                            return channel;
                        }
                );
    }

    private Optional<TwoPartyPrivateChatChannel> findChannel(ChatChannelDomain chatChannelDomain, UserProfile peer, String myUserIdentityId) {
        return findChannel(TwoPartyPrivateChatChannel.createId(chatChannelDomain, peer.getId(), myUserIdentityId));
    }
}