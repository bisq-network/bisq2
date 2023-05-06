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
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.persistence.PersistableStore;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class PrivateGroupChatChannelService<M extends PrivateChatMessage,
        C extends PrivateGroupChatChannel<M>, S extends PersistableStore<S>>
        extends PrivateChatChannelService<M, C, S> implements MessageListener {

    public PrivateGroupChatChannelService(NetworkService networkService,
                                          UserIdentityService userIdentityService,
                                          UserProfileService userProfileService,
                                          ProofOfWorkService proofOfWorkService,
                                          ChatChannelDomain chatChannelDomain) {
        super(networkService, userIdentityService, userProfileService, proofOfWorkService, chatChannelDomain);
    }

    @Override
    public void leaveChannel(C channel) {
        channel.getPeers().forEach(peer -> leaveChannel(channel, peer));
        // todo 
        //channel.getChannelMembers().remove()
    }
}