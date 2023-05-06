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

package bisq.chat.channel;

import bisq.chat.message.MessageType;
import bisq.chat.message.Quotation;
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
public class PrivateTwoPartyChannelService extends PrivateChatChannelService<TwoPartyPrivateChatMessage, TwoPartyPrivateChatChannel, PrivateTwoPartyChannelStore> {
    @Getter
    private final PrivateTwoPartyChannelStore persistableStore = new PrivateTwoPartyChannelStore();
    @Getter
    private final Persistence<PrivateTwoPartyChannelStore> persistence;

    public PrivateTwoPartyChannelService(PersistenceService persistenceService,
                                         NetworkService networkService,
                                         UserIdentityService userIdentityService,
                                         UserProfileService userProfileService,
                                         ProofOfWorkService proofOfWorkService,
                                         ChannelDomain channelDomain) {
        super(networkService, userIdentityService, userProfileService, proofOfWorkService, channelDomain);
        persistence = persistenceService.getOrCreatePersistence(this,
                "db",
                "Private" + StringUtils.capitalize(channelDomain.name()) + "ChannelStore",
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
                                                                     TwoPartyPrivateChatChannel channel,
                                                                     UserProfile sender,
                                                                     String receiversId,
                                                                     String text,
                                                                     Optional<Quotation> quotedMessage,
                                                                     long time,
                                                                     boolean wasEdited,
                                                                     MessageType messageType) {
        return new TwoPartyPrivateChatMessage(messageId,
                channel.getChannelDomain(),
                channel.getChannelName(),
                sender,
                receiversId,
                text,
                quotedMessage,
                new Date().getTime(),
                wasEdited,
                messageType);
    }

    @Override
    protected TwoPartyPrivateChatChannel createNewChannel(UserProfile peer, UserIdentity myUserIdentity) {
        TwoPartyPrivateChatChannel twoPartyPrivateChatChannel = new TwoPartyPrivateChatChannel(peer, myUserIdentity, channelDomain);
        twoPartyPrivateChatChannel.getChannelNotificationType().addObserver(value -> persist());
        return twoPartyPrivateChatChannel;
    }

    @Override
    public ObservableArray<TwoPartyPrivateChatChannel> getChannels() {
        return persistableStore.getChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<TwoPartyPrivateChatChannel> maybeCreateAndAddChannel(UserProfile peer) {
        return Optional.ofNullable(userIdentityService.getSelectedUserIdentity().get())
                .flatMap(myUserIdentity -> maybeCreateAndAddChannel(peer, myUserIdentity.getId()));
    }

    @Override
    public void leaveChannel(TwoPartyPrivateChatChannel channel) {
        leaveChannel(channel, channel.getPeer());
        // todo 
        //channel.getChannelMembers().remove()
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendTextMessage(String text,
                                                                               Optional<Quotation> quotedMessage,
                                                                               TwoPartyPrivateChatChannel channel) {
        return sendMessage(StringUtils.createShortUid(), text, quotedMessage, channel, channel.getPeer(), MessageType.TEXT);
    }

    protected Optional<TwoPartyPrivateChatChannel> maybeCreateAndAddChannel(UserProfile peer, String myUserIdentityId) {
        return userIdentityService.findUserIdentity(myUserIdentityId)
                .map(myUserIdentity -> {
                    Optional<TwoPartyPrivateChatChannel> existingChannel = getChannels().stream()
                            .filter(channel -> channel.getMyUserIdentity().equals(myUserIdentity) &&
                                    channel.getPeer().equals(peer))
                            .findAny();
                    if (existingChannel.isPresent()) {
                        return existingChannel.get();
                    }

                    TwoPartyPrivateChatChannel channel = createNewChannel(peer, myUserIdentity);
                            getChannels().add(channel);
                            persist();
                            return channel;
                        }
                );
    }

    protected void processMessage(TwoPartyPrivateChatMessage message) {
        if (message.getChannelDomain() != channelDomain) {
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