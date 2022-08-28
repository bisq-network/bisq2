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

import bisq.chat.message.PrivateChatMessage;
import bisq.chat.message.Quotation;
import bisq.common.util.StringUtils;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.persistence.PersistableStore;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public abstract class PrivateChannelService<M extends PrivateChatMessage, C extends PrivateChannel<M>, S extends PersistableStore<S>>
        extends ChannelService<M, C, S> implements MessageListener {
    protected final ProofOfWorkService proofOfWorkService;

    public PrivateChannelService(NetworkService networkService,
                                 UserIdentityService userIdentityService,
                                 ProofOfWorkService proofOfWorkService) {
        super(networkService, userIdentityService);
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

    public CompletableFuture<NetworkService.SendMessageResult> sendPrivateChatMessage(String text,
                                                                                      Optional<Quotation> quotedMessage,
                                                                                      C channel) {
        return sendPrivateChatMessage(StringUtils.createShortUid(), text, quotedMessage, channel, channel.getMyUserIdentity(), channel.getPeer());
    }

    protected CompletableFuture<NetworkService.SendMessageResult> sendPrivateChatMessage(String messageId,
                                                                                         String text,
                                                                                         Optional<Quotation> quotedMessage,
                                                                                         C channel,
                                                                                         UserIdentity senderIdentity,
                                                                                         UserProfile receiver) {
        M chatMessage = createNewPrivateChatMessage(messageId,
                channel,
                senderIdentity.getUserProfile(),
                receiver.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addMessage(chatMessage, channel);
        NetworkId receiverNetworkId = receiver.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = senderIdentity.getNodeIdAndKeyPair();
        return networkService.confidentialSend(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    public void removeExpiredMessages(C channel) {
        Set<M> toRemove = channel.getChatMessages().stream()
                .filter(PrivateChatMessage::isExpired)
                .collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            synchronized (getPersistableStore()) {
                channel.removeChatMessages(toRemove);
            }
            persist();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected Optional<C> maybeCreateAndAddChannel(UserProfile peer, String myUserIdentityId) {
        return userIdentityService.findUserIdentity(myUserIdentityId)
                .map(myUserIdentity -> {
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
                                                     boolean wasEdited);

    protected void processMessage(M message) {
        if (!userIdentityService.isUserIdentityPresent(message.getAuthorId()) &&
                proofOfWorkService.verify(message.getSender().getProofOfWork())) {
            findChannel(message.getChannelId())
                    .or(() -> maybeCreateAndAddChannel(message.getSender(), message.getReceiversId()))
                    .ifPresent(channel -> addMessage(message, channel));
        }
    }
}