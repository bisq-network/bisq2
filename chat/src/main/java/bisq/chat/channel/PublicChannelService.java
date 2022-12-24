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

import bisq.chat.message.PublicChatMessage;
import bisq.chat.message.Quotation;
import bisq.common.observable.ObservableArray;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
public class PublicChannelService extends BasePublicChannelService<PublicChatMessage, PublicChannel, PublicChannelStore> {
    @Getter
    private final PublicChannelStore persistableStore = new PublicChannelStore();
    @Getter
    private final Persistence<PublicChannelStore> persistence;
    private final List<PublicChannel> defaultChannels;

    public PublicChannelService(PersistenceService persistenceService,
                                NetworkService networkService,
                                UserIdentityService userIdentityService,
                                UserProfileService userProfileService,
                                ChannelDomain channelDomain,
                                List<PublicChannel> defaultChannels) {
        super(networkService, userIdentityService, userProfileService, channelDomain);

        this.defaultChannels = defaultChannels;

        persistence = persistenceService.getOrCreatePersistence(this,
                "db",
                "Public" + StringUtils.capitalize(channelDomain.name()) + "ChannelStore",
                persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicChatMessage) {
            processAddedMessage((PublicChatMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicChatMessage) {
            processRemovedMessage((PublicChatMessage) distributedData);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PublicChannelService 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableArray<PublicChannel> getChannels() {
        return persistableStore.getChannels();
    }

    @Override
    protected PublicChatMessage createChatMessage(String text,
                                                  Optional<Quotation> quotedMessage,
                                                  PublicChannel publicChannel,
                                                  UserProfile userProfile) {
        return new PublicChatMessage(publicChannel.getChannelDomain(),
                publicChannel.getChannelName(),
                userProfile.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
    }

    @Override
    protected PublicChatMessage createEditedChatMessage(PublicChatMessage originalChatMessage,
                                                        String editedText,
                                                        UserProfile userProfile) {
        return new PublicChatMessage(originalChatMessage.getChannelDomain(),
                originalChatMessage.getChannelName(),
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