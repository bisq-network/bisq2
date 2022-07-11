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

package bisq.chat;

import bisq.chat.channels.*;
import bisq.chat.messages.ChatMessage;
import bisq.common.observable.Observable;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * Manages chatChannels and persistence of the chatModel.
 * ChatUser and ChatIdentity management is not implemented yet. Not 100% clear yet if ChatIdentity management should
 * be rather part of the identity module.
 */
@Slf4j
public class ChatService implements PersistenceClient<ChatStore> {
    @Getter
    private final ChatStore persistableStore = new ChatStore();
    @Getter
    private final Persistence<ChatStore> persistence;
    private final UserIdentityService userIdentityService;
    private final NetworkService networkService;
    private final ProofOfWorkService proofOfWorkService;
    // private final Map<String, UserProfile> chatUserById = new ConcurrentHashMap<>();
    @Getter
    private final PrivateTradeChannelService privateTradeChannelService;
    @Getter
    private final PrivateDiscussionChannelService privateDiscussionChannelService;
    @Getter
    private final PublicDiscussionChannelService publicDiscussionChannelService;
    @Getter
    private final PublicTradeChannelService publicTradeChannelService;

    public ChatService(PersistenceService persistenceService,
                       ProofOfWorkService proofOfWorkService,
                       NetworkService networkService,
                       UserIdentityService userIdentityService) {
        this.proofOfWorkService = proofOfWorkService;
        this.networkService = networkService;
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.userIdentityService = userIdentityService;

        privateTradeChannelService = new PrivateTradeChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService);
        privateDiscussionChannelService = new PrivateDiscussionChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService);
        publicDiscussionChannelService = new PublicDiscussionChannelService(persistenceService,
                networkService,
                userIdentityService);
        publicTradeChannelService = new PublicTradeChannelService(persistenceService,
                networkService,
                userIdentityService);

    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFutureUtils.allOf(
                privateTradeChannelService.initialize(),
                privateDiscussionChannelService.initialize(),
                publicDiscussionChannelService.initialize(),
                publicTradeChannelService.initialize()
        ).thenApply(list -> {
            maybeAddDefaultChannels();
            return true;
        });
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        privateTradeChannelService.shutdown();
        privateDiscussionChannelService.shutdown();
        publicDiscussionChannelService.shutdown();
        publicTradeChannelService.shutdown();
        return CompletableFuture.completedFuture(true);
    }

    public void selectTradeChannel(Channel<? extends ChatMessage> channel) {
        if (channel instanceof PrivateTradeChannel) {
            privateTradeChannelService.removeExpiredMessages((PrivateTradeChannel) channel);
        }

        getSelectedTradeChannel().set(channel);
        persist();
    }

    public Observable<Channel<? extends ChatMessage>> getSelectedTradeChannel() {
        return persistableStore.getSelectedTradeChannel();
    }


    public void selectDiscussionChannel(Channel<? extends ChatMessage> channel) {
        if (channel instanceof PrivateDiscussionChannel) {
            privateDiscussionChannelService.removeExpiredMessages((PrivateDiscussionChannel) channel);
        }
        getSelectedDiscussionChannel().set(channel);
        persist();
    }

    public Observable<Channel<? extends ChatMessage>> getSelectedDiscussionChannel() {
        return persistableStore.getSelectedDiscussionChannel();
    }


    public void reportUserProfile(UserProfile userProfile, String reason) {
        //todo report user to admin and moderators, add reason
        log.info("called reportChatUser {} {}", userProfile, reason);
    }

    private void maybeAddDefaultChannels() {
        if (getSelectedTradeChannel().get() == null) {
            publicTradeChannelService.getChannels().stream().findAny().ifPresent(this::selectTradeChannel);
        }
        if (getSelectedDiscussionChannel().get() == null) {
            publicDiscussionChannelService.getChannels().stream().findAny().ifPresent(this::selectDiscussionChannel);
        }
        persist();
    }
}