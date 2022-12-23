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

package bisq.chat.events.pub;

import bisq.chat.ChatDomain;
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
import java.util.Optional;

@Slf4j
public class PublicEventsChannelService extends PublicChannelService<PublicEventsChatMessage, PublicEventsChannel, PublicEventsChannelStore> {
    @Getter
    private final PublicEventsChannelStore persistableStore = new PublicEventsChannelStore();
    @Getter
    private final Persistence<PublicEventsChannelStore> persistence;

    public PublicEventsChannelService(PersistenceService persistenceService,
                                      NetworkService networkService,
                                      UserIdentityService userIdentityService) {
        super(networkService, userIdentityService, ChatDomain.EVENTS);

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicEventsChatMessage) {
            processAddedMessage((PublicEventsChatMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicEventsChatMessage) {
            processRemovedMessage((PublicEventsChatMessage) distributedData);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PublicChannelService 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableArray<PublicEventsChannel> getChannels() {
        return persistableStore.getChannels();
    }

    @Override
    protected PublicEventsChatMessage createChatMessage(String text,
                                                        Optional<Quotation> quotedMessage,
                                                        PublicEventsChannel publicChannel,
                                                        UserProfile userProfile) {
        return new PublicEventsChatMessage(publicChannel.getId(),
                userProfile.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
    }

    @Override
    protected PublicEventsChatMessage createEditedChatMessage(PublicEventsChatMessage originalChatMessage,
                                                              String editedText,
                                                              UserProfile userProfile) {
        return new PublicEventsChatMessage(originalChatMessage.getChannelId(),
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
        PublicEventsChannel defaultEventsChannel = new PublicEventsChannel("conferences");
        ObservableArray<PublicEventsChannel> channels = getChannels();
        channels.add(defaultEventsChannel);
        channels.add(new PublicEventsChannel("meetups"));
        channels.add(new PublicEventsChannel("podcasts"));
      /*  channels.add(new PublicEventsChannel("noKyc"));
        channels.add(new PublicEventsChannel("nodes"));*/
        channels.add(new PublicEventsChannel("tradeEvents"));
        persist();
    }
}