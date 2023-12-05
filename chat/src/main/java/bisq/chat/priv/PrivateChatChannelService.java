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

package bisq.chat.priv;

import bisq.chat.*;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.identity.TorIdentity;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.persistence.PersistableStore;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class PrivateChatChannelService<
        M extends PrivateChatMessage,
        C extends PrivateChatChannel<M>,
        S extends PersistableStore<S>
        > extends ChatChannelService<M, C, S> implements MessageListener {
    protected final ProofOfWorkService proofOfWorkService;

    public PrivateChatChannelService(NetworkService networkService,
                                     UserService userService,
                                     ProofOfWorkService proofOfWorkService,
                                     ChatChannelDomain chatChannelDomain) {
        super(networkService, userService, chatChannelDomain);

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

    protected CompletableFuture<SendMessageResult> sendMessage(String messageId,
                                                               @Nullable String text,
                                                               Optional<Citation> citation,
                                                               C channel,
                                                               UserProfile receiver,
                                                               ChatMessageType chatMessageType,
                                                               long date) {
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        if (bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile())) {
            return CompletableFuture.failedFuture(new RuntimeException());
        }
        if (isPeerBanned(receiver)) {
            return CompletableFuture.failedFuture(new RuntimeException("Peer is banned"));
        }

        M chatMessage = createAndGetNewPrivateChatMessage(messageId,
                channel,
                myUserIdentity.getUserProfile(),
                receiver,
                text,
                citation,
                date,
                false,
                chatMessageType);
        addMessage(chatMessage, channel);
        NetworkId receiverNetworkId = receiver.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        TorIdentity senderTorIdentity = myUserIdentity.getIdentity().getTorIdentity();
        return networkService.confidentialSend(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair, senderTorIdentity);
    }

    protected boolean isPeerBanned(UserProfile userProfile) {
        return bannedUserService.isUserProfileBanned(userProfile);
    }

    public void leaveChannel(String id) {
        findChannel(id).ifPresent(this::leaveChannel);
    }

    public void leaveChannel(C channel) {
        synchronized (this) {
            getChannels().remove(channel);
        }
        persist();
    }

    @Override
    public Optional<C> getDefaultChannel() {
        return Optional.empty();
    }

    protected CompletableFuture<SendMessageResult> sendLeaveMessage(C channel, UserProfile receiver, long date) {
        return sendMessage(StringUtils.createUid(),
                Res.get("chat.privateChannel.message.leave", channel.getMyUserIdentity().getUserProfile().getUserName()),
                Optional.empty(),
                channel,
                receiver,
                ChatMessageType.LEAVE,
                date);
    }

    @Override
    public String getChannelTitlePostFix(ChatChannel<? extends ChatMessage> chatChannel) {
        checkArgument(chatChannel instanceof PrivateChatChannel,
                "chatChannel at PrivateChatChannelService.getChannelTitlePostFix must be of type PrivateChatChannel");
        return userIdentityService.hasMultipleUserIdentities() ? "" :
                " [" + ((PrivateChatChannel<?>) chatChannel).getMyUserIdentity().getUserName() + "]";
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract void processMessage(M message);

    @Override
    protected boolean isValid(M message) {
        if (message.isMyMessage(userIdentityService)) {
            log.warn("Sent a message to myself. This should never happen for private chat messages.");
            return false;
        }
        if (bannedUserService.isNetworkIdBanned(message.getSenderUserProfile().getNetworkId())) {
            log.warn("Message invalid as sender is banned");
            return false;
        }
        return super.isValid(message);
    }

    protected abstract C createAndGetNewPrivateChatChannel(UserProfile peer, UserIdentity myUserIdentity);

    protected abstract M createAndGetNewPrivateChatMessage(String messageId,
                                                           C channel,
                                                           UserProfile senderUserProfile,
                                                           UserProfile receiverUserProfile,
                                                           String text,
                                                           Optional<Citation> citation,
                                                           long time,
                                                           boolean wasEdited,
                                                           ChatMessageType chatMessageType);
}