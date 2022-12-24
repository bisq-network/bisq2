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
import bisq.chat.message.PrivateChatMessage;
import bisq.chat.message.Quotation;
import bisq.common.observable.ObservableArray;
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

@Slf4j
public class PrivateChannelService extends BasePrivateChannelService<PrivateChatMessage, PrivateChannel, PrivateChannelStore> {
    @Getter
    private final PrivateChannelStore persistableStore = new PrivateChannelStore();
    @Getter
    private final Persistence<PrivateChannelStore> persistence;

    public PrivateChannelService(PersistenceService persistenceService,
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
        if (networkMessage instanceof PrivateChatMessage) {
            processMessage((PrivateChatMessage) networkMessage);
        }
    }

    @Override
    protected PrivateChatMessage createNewPrivateChatMessage(String messageId,
                                                             PrivateChannel channel,
                                                             UserProfile sender,
                                                             String receiversId,
                                                             String text,
                                                             Optional<Quotation> quotedMessage,
                                                             long time,
                                                             boolean wasEdited,
                                                             MessageType messageType) {
        return new PrivateChatMessage(messageId,
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
    protected PrivateChannel createNewChannel(UserProfile peer, UserIdentity myUserIdentity) {
        return new PrivateChannel(peer, myUserIdentity, channelDomain);
    }

    @Override
    public ObservableArray<PrivateChannel> getChannels() {
        return persistableStore.getChannels();
    }
}