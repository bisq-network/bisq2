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

package bisq.chat.channels;

import bisq.chat.messages.PrivateTradeChatMessage;
import bisq.chat.messages.Quotation;
import bisq.common.observable.ObservableSet;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class PrivateTradeChannelService extends ChannelService<PrivateTradeChannel>
        implements PersistenceClient<PrivateTradeChannelStore>, MessageListener {
    @Getter
    private final PrivateTradeChannelStore persistableStore = new PrivateTradeChannelStore();
    @Getter
    private final Persistence<PrivateTradeChannelStore> persistence;
    private final ProofOfWorkService proofOfWorkService;

    public PrivateTradeChannelService(PersistenceService persistenceService,
                                      NetworkService networkService,
                                      UserIdentityService userIdentityService,
                                      ProofOfWorkService proofOfWorkService) {
        super(networkService, userIdentityService);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.proofOfWorkService = proofOfWorkService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof PrivateTradeChatMessage) {
            PrivateTradeChatMessage message = (PrivateTradeChatMessage) networkMessage;
            if (!isMyMessage(message) && hasAuthorValidProofOfWork(message.getSender().getProofOfWork())) {
                processMessage(message);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    public CompletableFuture<NetworkService.SendMessageResult> sendPrivateTradeChatMessage(String text,
                                                                                           Optional<Quotation> quotedMessage,
                                                                                           PrivateTradeChannel privateTradeChannel) {
        String channelId = privateTradeChannel.getId();
        UserIdentity userIdentity = privateTradeChannel.getMyProfile();
        UserProfile peer = privateTradeChannel.getPeer();
        PrivateTradeChatMessage chatMessage = new PrivateTradeChatMessage(channelId,
                userIdentity.getUserProfile(),
                peer.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addPrivateTradeChatMessage(chatMessage, privateTradeChannel);
        NetworkId receiverNetworkId = peer.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = userIdentity.getNodeIdAndKeyPair();
        return networkService.sendMessage(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }


    private void processMessage(PrivateTradeChatMessage message) {
        findChannel(message.getChannelId())
                .or(() -> createPrivateTradeChannel(message.getSender(), message.getReceiversId()))
                .ifPresent(channel -> addPrivateTradeChatMessage(message, channel));
    }

    public Optional<PrivateTradeChannel> createPrivateTradeChannel(UserProfile peer) {
        return Optional.ofNullable(userIdentityService.getSelectedUserProfile().get())
                .flatMap(userProfile -> createPrivateTradeChannel(peer, userProfile.getId()));
    }

    // We received the message so the receiversId is out id.
    private Optional<PrivateTradeChannel> createPrivateTradeChannel(UserProfile peer, String peersId) {
        return userIdentityService.findUserIdentity(peersId)
                .map(myUserProfile -> {
                            PrivateTradeChannel privateTradeChannel = new PrivateTradeChannel(peer, myUserProfile);
                            getChannels().add(privateTradeChannel);
                            persist();
                            return privateTradeChannel;
                        }
                );
    }

    private void addPrivateTradeChatMessage(PrivateTradeChatMessage chatMessage, PrivateTradeChannel privateTradeChannel) {
        synchronized (persistableStore) {
            privateTradeChannel.addChatMessage(chatMessage);
        }
        persist();
    }

    @Override
    public ObservableSet<PrivateTradeChannel> getChannels() {
        return persistableStore.getChannels();
    }

    public void removeExpiredMessages(PrivateTradeChannel channel) {
        Set<PrivateTradeChatMessage> toRemove = channel.getChatMessages().stream()
                .filter(PrivateTradeChatMessage::isExpired)
                .collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            synchronized (persistableStore) {
                channel.removeChatMessages(toRemove);
            }
            persist();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean hasAuthorValidProofOfWork(ProofOfWork proofOfWork) {
        return proofOfWorkService.verify(proofOfWork);
    }

}