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

package bisq.chat.channel.priv;

import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.message.ChatMessageType;
import bisq.chat.message.Citation;
import bisq.chat.message.TwoPartyPrivateChatMessage;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PrivateTwoPartyChatChannelService extends PrivateChatChannelService<TwoPartyPrivateChatMessage, PrivateTwoPartyChatChannel, PrivateTwoPartyChatChannelStore> {
    @Getter
    private final PrivateTwoPartyChatChannelStore persistableStore = new PrivateTwoPartyChatChannelStore();
    @Getter
    private final Persistence<PrivateTwoPartyChatChannelStore> persistence;

    public PrivateTwoPartyChatChannelService(PersistenceService persistenceService,
                                             NetworkService networkService,
                                             UserIdentityService userIdentityService,
                                             UserProfileService userProfileService,
                                             ProofOfWorkService proofOfWorkService,
                                             ChatChannelDomain chatChannelDomain) {
        super(networkService, userIdentityService, userProfileService, proofOfWorkService, chatChannelDomain);
        persistence = persistenceService.getOrCreatePersistence(this,
                "db",
                "Private" + StringUtils.capitalize(chatChannelDomain.name()) + "ChannelStore",
                persistableStore);
    }

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof TwoPartyPrivateChatMessage) {
            processMessage((TwoPartyPrivateChatMessage) networkMessage);
        }
    }

    @Override
    protected TwoPartyPrivateChatMessage createNewPrivateChatMessage(String messageId,
                                                                     PrivateTwoPartyChatChannel channel,
                                                                     UserProfile sender,
                                                                     String receiversId,
                                                                     String text,
                                                                     Optional<Citation> citation,
                                                                     long time,
                                                                     boolean wasEdited,
                                                                     ChatMessageType chatMessageType) {
        return new TwoPartyPrivateChatMessage(messageId,
                channel.getChatChannelDomain(),
                channel.getChannelName(),
                sender,
                receiversId,
                text,
                citation,
                new Date().getTime(),
                wasEdited,
                chatMessageType);
    }

    @Override
    protected PrivateTwoPartyChatChannel createNewChannel(UserProfile peer, UserIdentity myUserIdentity) {
        PrivateTwoPartyChatChannel privateTwoPartyChatChannel = new PrivateTwoPartyChatChannel(peer, myUserIdentity, chatChannelDomain);
        privateTwoPartyChatChannel.getChatChannelNotificationType().addObserver(value -> persist());
        return privateTwoPartyChatChannel;
    }

    @Override
    public ObservableArray<PrivateTwoPartyChatChannel> getChannels() {
        return persistableStore.getChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<PrivateTwoPartyChatChannel> maybeCreateAndAddChannel(UserProfile peer) {
        return Optional.ofNullable(userIdentityService.getSelectedUserIdentity().get())
                .flatMap(myUserIdentity -> maybeCreateAndAddChannel(peer, myUserIdentity.getId()));
    }

    @Override
    public void leaveChannel(PrivateTwoPartyChatChannel channel) {
        leaveChannel(channel, channel.getPeer());
        // todo 
        //channel.getChannelMembers().remove()
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendTextMessage(String text,
                                                                               Optional<Citation> citation,
                                                                               PrivateTwoPartyChatChannel channel) {
        return sendMessage(StringUtils.createShortUid(), text, citation, channel, channel.getPeer(), ChatMessageType.TEXT);
    }

    protected Optional<PrivateTwoPartyChatChannel> maybeCreateAndAddChannel(UserProfile peer, String myUserIdentityId) {
        return userIdentityService.findUserIdentity(myUserIdentityId)
                .map(myUserIdentity -> {
                    Optional<PrivateTwoPartyChatChannel> existingChannel = getChannels().stream()
                            .filter(channel -> channel.getMyUserIdentity().equals(myUserIdentity) &&
                                    channel.getPeer().equals(peer))
                            .findAny();
                    if (existingChannel.isPresent()) {
                        return existingChannel.get();
                    }

                    PrivateTwoPartyChatChannel channel = createNewChannel(peer, myUserIdentity);
                            getChannels().add(channel);
                            persist();
                            return channel;
                        }
                );
    }

    protected void processMessage(TwoPartyPrivateChatMessage message) {
        if (message.getChatChannelDomain() != chatChannelDomain) {
            return;
        }
        boolean isMyMessage = userIdentityService.isUserIdentityPresent(message.getAuthorId());
        if (!isMyMessage) {
            findChannelForMessage(message)
                    .or(() -> maybeCreateAndAddChannel(message.getSender(), message.getReceiversId()))
                    .ifPresent(channel -> addMessage(message, channel));
        }
    }
}