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

import bisq.chat.message.BasePrivateChatMessage;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.MessageType;
import bisq.chat.message.Quotation;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.persistence.PersistableStore;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public abstract class BasePrivateChannelService<M extends BasePrivateChatMessage, C extends BasePrivateChannel<M>, S extends PersistableStore<S>>
        extends ChannelService<M, C, S> implements MessageListener {
    protected final ProofOfWorkService proofOfWorkService;

    public BasePrivateChannelService(NetworkService networkService,
                                     UserIdentityService userIdentityService,
                                     UserProfileService userProfileService,
                                     ProofOfWorkService proofOfWorkService,
                                     ChannelDomain channelDomain) {
        super(networkService, userIdentityService, userProfileService, channelDomain);
        this.proofOfWorkService = proofOfWorkService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<C> maybeCreateAndAddChannel(UserProfile peer) {
        return Optional.ofNullable(userIdentityService.getSelectedUserIdentity().get())
                .flatMap(myUserIdentity -> maybeCreateAndAddChannel(peer, myUserIdentity.getId()));
    }

    protected CompletableFuture<NetworkService.SendMessageResult> sendLeaveMessage(C channel) {
        return sendPrivateChatMessage(Res.get("social.privateChannel.leave.message", channel.getMyUserIdentity().getUserProfile().getUserName()),
                Optional.empty(),
                channel,
                MessageType.LEAVE);
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendPrivateChatMessage(String text,
                                                                                      Optional<Quotation> quotedMessage,
                                                                                      C channel) {
        return sendPrivateChatMessage(StringUtils.createShortUid(), text, quotedMessage, channel, channel.getMyUserIdentity(), channel.getPeer(), MessageType.TEXT);
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendPrivateChatMessage(String text,
                                                                                      Optional<Quotation> quotedMessage,
                                                                                      C channel,
                                                                                      MessageType messageType) {
        return sendPrivateChatMessage(StringUtils.createShortUid(), text, quotedMessage, channel, channel.getMyUserIdentity(), channel.getPeer(), messageType);
    }

    protected CompletableFuture<NetworkService.SendMessageResult> sendPrivateChatMessage(String messageId,
                                                                                         String text,
                                                                                         Optional<Quotation> quotedMessage,
                                                                                         C channel,
                                                                                         UserIdentity senderIdentity,
                                                                                         UserProfile receiver,
                                                                                         MessageType messageType) {
        M chatMessage = createNewPrivateChatMessage(messageId,
                channel,
                senderIdentity.getUserProfile(),
                receiver.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false,
                messageType);
        addMessage(chatMessage, channel);
        NetworkId receiverNetworkId = receiver.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = senderIdentity.getNodeIdAndKeyPair();
        return networkService.confidentialSend(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    public void removeExpiredMessages(C channel) {
        Set<M> toRemove = channel.getChatMessages().stream()
                .filter(BasePrivateChatMessage::isExpired)
                .collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            synchronized (getPersistableStore()) {
                channel.removeChatMessages(toRemove);
            }
            persist();
        }
    }

    public void leaveChannel(C channel) {
        if (channel.getChatMessages().stream()
                .max(Comparator.comparing(ChatMessage::getDate))
                .stream().anyMatch(m -> m.getMessageType().equals(MessageType.LEAVE))) {
            // Don't send leave message if peer already left channel
            getChannels().remove(channel);
            return;
        }

        sendLeaveMessage(channel)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("Sending leave channel message failed.");
                    }
                    getChannels().remove(channel);
                    persist();
                });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected Optional<C> maybeCreateAndAddChannel(UserProfile peer, String myUserIdentityId) {
        return userIdentityService.findUserIdentity(myUserIdentityId)
                .map(myUserIdentity -> {
                            Optional<C> existingChannel = getChannels().stream()
                                    .filter(channel -> channel.getMyUserIdentity().equals(myUserIdentity) &&
                                            channel.getPeer().equals(peer))
                                    .findAny();
                            if (existingChannel.isPresent()) {
                                return existingChannel.get();
                            }

                            C channel = createNewChannel(peer, myUserIdentity);
                            getChannels().add(channel);
                            persist();
                            return channel;
                        }
                );
    }

    protected abstract C createNewChannel(UserProfile peer, UserIdentity myUserIdentity);

    protected abstract M createNewPrivateChatMessage(String messageId,
                                                     C channel,
                                                     UserProfile sender,
                                                     String receiversId,
                                                     String text,
                                                     Optional<Quotation> quotedMessage,
                                                     long time,
                                                     boolean wasEdited,
                                                     MessageType messageType);

    protected void processMessage(M message) {
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