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

package bisq.chat.channel.private_two_party;

import bisq.chat.ChatDomain;
import bisq.chat.channel.PrivateChannelService;
import bisq.chat.message.MessageType;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;

@Slf4j
public class PrivateTwoPartyChannelService extends PrivateChannelService<PrivateTwoPartyChatMessage, PrivateTwoPartyChannel, PrivateTwoPartyChannelStore> {
    @Getter
    private final PrivateTwoPartyChannelStore persistableStore = new PrivateTwoPartyChannelStore();
    @Getter
    private final Persistence<PrivateTwoPartyChannelStore> persistence;

    public PrivateTwoPartyChannelService(PersistenceService persistenceService,
                                         NetworkService networkService,
                                         UserIdentityService userIdentityService,
                                         ProofOfWorkService proofOfWorkService,
                                         ChatDomain chatDomain) {
        super(networkService, userIdentityService, proofOfWorkService, chatDomain);
        persistence = persistenceService.getOrCreatePersistence(this,
                "db",
                "Private" + StringUtils.capitalize(chatDomain.name()) + "ChannelStore",
                persistableStore);
    }

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof PrivateTwoPartyChatMessage) {
            processMessage((PrivateTwoPartyChatMessage) networkMessage);
        }
    }

    @Override
    protected PrivateTwoPartyChatMessage createNewPrivateChatMessage(String messageId,
                                                                     PrivateTwoPartyChannel channel,
                                                                     UserProfile sender,
                                                                     String receiversId,
                                                                     String text,
                                                                     Optional<Quotation> quotedMessage,
                                                                     long time,
                                                                     boolean wasEdited,
                                                                     MessageType messageType) {
        return new PrivateTwoPartyChatMessage(messageId,
                channel.getId(),
                sender,
                receiversId,
                text,
                quotedMessage,
                new Date().getTime(),
                wasEdited,
                messageType);
    }

    @Override
    protected PrivateTwoPartyChannel createNewChannel(UserProfile peer, UserIdentity myUserIdentity) {
        return new PrivateTwoPartyChannel(peer, myUserIdentity, chatDomain);
    }

    @Override
    public ObservableArray<PrivateTwoPartyChannel> getChannels() {
        return persistableStore.getChannels();
    }
}