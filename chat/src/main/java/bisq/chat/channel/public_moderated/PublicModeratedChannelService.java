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

package bisq.chat.channel.public_moderated;

import bisq.chat.channel.ChannelDomain;
import bisq.chat.channel.PublicChannelService;
import bisq.chat.message.Quotation;
import bisq.common.observable.ObservableArray;
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
import java.util.List;
import java.util.Optional;

@Slf4j
public class PublicModeratedChannelService extends PublicChannelService<PublicModeratedChatMessage, PublicModeratedChannel, PublicModeratedChannelStore> {
    @Getter
    private final PublicModeratedChannelStore persistableStore = new PublicModeratedChannelStore();
    @Getter
    private final Persistence<PublicModeratedChannelStore> persistence;
    private final List<PublicModeratedChannel> defaultChannels;

    public PublicModeratedChannelService(PersistenceService persistenceService,
                                         NetworkService networkService,
                                         UserIdentityService userIdentityService,
                                         ChannelDomain channelDomain,
                                         List<PublicModeratedChannel> defaultChannels) {
        super(networkService, userIdentityService, channelDomain);

        this.defaultChannels = defaultChannels;

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicModeratedChatMessage) {
            processAddedMessage((PublicModeratedChatMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicModeratedChatMessage) {
            processRemovedMessage((PublicModeratedChatMessage) distributedData);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PublicChannelService 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableArray<PublicModeratedChannel> getChannels() {
        return persistableStore.getChannels();
    }

    @Override
    protected PublicModeratedChatMessage createChatMessage(String text,
                                                           Optional<Quotation> quotedMessage,
                                                           PublicModeratedChannel publicChannel,
                                                           UserProfile userProfile) {
        return new PublicModeratedChatMessage(publicChannel.getId(),
                userProfile.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
    }

    @Override
    protected PublicModeratedChatMessage createEditedChatMessage(PublicModeratedChatMessage originalChatMessage,
                                                                 String editedText,
                                                                 UserProfile userProfile) {
        return new PublicModeratedChatMessage(originalChatMessage.getChannelId(),
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

        getChannels().addAll(defaultChannels);
        persist();
    }
}