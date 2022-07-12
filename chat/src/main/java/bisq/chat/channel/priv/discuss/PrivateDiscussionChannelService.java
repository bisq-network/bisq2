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

package bisq.chat.channel.priv.discuss;

import bisq.chat.channel.priv.PrivateChannelService;
import bisq.chat.message.PrivateDiscussionChatMessage;
import bisq.chat.message.Quotation;
import bisq.common.observable.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;

@Slf4j
public class PrivateDiscussionChannelService extends PrivateChannelService<PrivateDiscussionChatMessage, PrivateDiscussionChannel, PrivateDiscussionChannelStore> {
    @Getter
    private final PrivateDiscussionChannelStore persistableStore = new PrivateDiscussionChannelStore();
    @Getter
    private final Persistence<PrivateDiscussionChannelStore> persistence;

    public PrivateDiscussionChannelService(PersistenceService persistenceService,
                                           NetworkService networkService,
                                           UserIdentityService userIdentityService,
                                           ProofOfWorkService proofOfWorkService) {
        super(networkService, userIdentityService, proofOfWorkService);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof PrivateDiscussionChatMessage) {
            processMessage((PrivateDiscussionChatMessage) networkMessage);
        }
    }

    @Override
    protected PrivateDiscussionChatMessage createNewPrivateChatMessage(String channelId,
                                                                       UserProfile sender,
                                                                       String receiversId,
                                                                       String text,
                                                                       Optional<Quotation> quotedMessage,
                                                                       long time,
                                                                       boolean wasEdited) {
        return new PrivateDiscussionChatMessage(channelId,
                sender,
                receiversId,
                text,
                quotedMessage,
                new Date().getTime(),
                wasEdited);
    }

    @Override
    protected PrivateDiscussionChannel createNewChannel(UserProfile peer, UserIdentity myUserIdentity) {
        return new PrivateDiscussionChannel(peer, myUserIdentity);
    }

    @Override
    public ObservableSet<PrivateDiscussionChannel> getChannels() {
        return persistableStore.getChannels();
    }
}