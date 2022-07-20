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

package bisq.chat.support.pub;

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
import java.util.Optional;

@Slf4j
public class PublicSupportChannelService extends PublicChannelService<PublicSupportChatMessage, PublicSupportChannel, PublicSupportChannelStore> {
    @Getter
    private final PublicSupportChannelStore persistableStore = new PublicSupportChannelStore();
    @Getter
    private final Persistence<PublicSupportChannelStore> persistence;

    public PublicSupportChannelService(PersistenceService persistenceService,
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
        if (distributedData instanceof PublicSupportChatMessage) {
            processAddedMessage((PublicSupportChatMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicSupportChatMessage) {
            processRemovedMessage((PublicSupportChatMessage) distributedData);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PublicChannelService 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableSet<PublicSupportChannel> getChannels() {
        return persistableStore.getChannels();
    }

    @Override
    protected PublicSupportChatMessage createNewChatMessage(String text,
                                                            Optional<Quotation> quotedMessage,
                                                            PublicSupportChannel publicChannel,
                                                            UserProfile userProfile) {
        return new PublicSupportChatMessage(publicChannel.getId(),
                userProfile.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
    }

    @Override
    protected PublicSupportChatMessage createNewChatMessage(PublicSupportChatMessage originalChatMessage,
                                                            String editedText,
                                                            UserProfile userProfile) {
        return new PublicSupportChatMessage(originalChatMessage.getChannelId(),
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

        PublicSupportChannel defaultSupportChannel = new PublicSupportChannel("support");
        ObservableSet<PublicSupportChannel> channels = getChannels();
        channels.add(defaultSupportChannel);
        channels.add(new PublicSupportChannel("questions"));
        channels.add(new PublicSupportChannel("reports"));
        persist();
    }
}