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

package bisq.chat.discuss.pub;

import bisq.chat.channel.PublicChannelService;
import bisq.chat.message.Quotation;
import bisq.common.observable.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

@Slf4j
public class PublicDiscussionChannelService extends PublicChannelService<PublicDiscussionChatMessage, PublicDiscussionChannel, PublicDiscussionChannelStore> {
    @Getter
    private final PublicDiscussionChannelStore persistableStore = new PublicDiscussionChannelStore();
    @Getter
    private final Persistence<PublicDiscussionChannelStore> persistence;

    public PublicDiscussionChannelService(PersistenceService persistenceService,
                                          NetworkService networkService,
                                          UserIdentityService userIdentityService) {
        super(networkService, userIdentityService);

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicDiscussionChatMessage) {
            processAddedMessage((PublicDiscussionChatMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicDiscussionChatMessage) {
            processRemovedMessage((PublicDiscussionChatMessage) distributedData);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PublicChannelService 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableSet<PublicDiscussionChannel> getChannels() {
        return persistableStore.getChannels();
    }

    @Override
    protected PublicDiscussionChatMessage createNewChatMessage(String text,
                                                               Optional<Quotation> quotedMessage,
                                                               PublicDiscussionChannel publicChannel,
                                                               UserProfile userProfile) {
        return new PublicDiscussionChatMessage(publicChannel.getId(),
                userProfile.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
    }

    @Override
    protected PublicDiscussionChatMessage createNewChatMessage(PublicDiscussionChatMessage originalChatMessage,
                                                               String editedText,
                                                               UserProfile userProfile) {
        return new PublicDiscussionChatMessage(originalChatMessage.getChannelId(),
                userProfile.getId(),
                editedText,
                originalChatMessage.getQuotation(),
                originalChatMessage.getDate(),
                true);
    }

    @Override
    protected void maybeAddDefaultChannels() {
        if (!getChannels().isEmpty()) {
            return;
        }
        // todo channelAdmin not supported atm
        String channelAdminId = "";
        PublicDiscussionChannel defaultDiscussionChannel = new PublicDiscussionChannel("discussion.bisq",
                "Discussions Bisq",
                "Channel for discussions about Bisq",
                channelAdminId,
                new HashSet<>()
        );
        ObservableSet<PublicDiscussionChannel> channels = getChannels();
        channels.add(defaultDiscussionChannel);
        channels.add(new PublicDiscussionChannel("discussion.bitcoin",
                "Discussions Bitcoin",
                "Channel for discussions about Bitcoin",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel("discussion.monero",
                "Discussions Monero",
                "Channel for discussions about Monero",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel("discussion.markets",
                "Markets",
                "Channel for discussions about markets and price",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel("discussion.economy",
                "Economy",
                "Channel for discussions about economy",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel("discussion.offTopic",
                "Off-topic",
                "Channel for anything else",
                channelAdminId,
                new HashSet<>()
        ));
        persist();
    }
}