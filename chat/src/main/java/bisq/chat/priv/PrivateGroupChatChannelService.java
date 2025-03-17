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

import bisq.chat.ChatChannelDomain;
import bisq.chat.reactions.PrivateChatMessageReaction;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.persistence.PersistableStore;
import bisq.user.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class PrivateGroupChatChannelService<
        R extends PrivateChatMessageReaction,
        M extends PrivateChatMessage<R>,
        C extends PrivateGroupChatChannel<M>,
        S extends PersistableStore<S>
        >
        extends PrivateChatChannelService<R, M, C, S> implements ConfidentialMessageService.Listener {

    public PrivateGroupChatChannelService(NetworkService networkService,
                                          UserService userService,
                                          ChatChannelDomain chatChannelDomain) {
        super(networkService, userService, chatChannelDomain);
    }
}
