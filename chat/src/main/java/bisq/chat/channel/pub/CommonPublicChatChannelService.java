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

package bisq.chat.channel.pub;

import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.message.CommonPublicChatMessage;
import bisq.chat.message.Quotation;
import bisq.common.observable.collection.ObservableArray;
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
public class CommonPublicChatChannelService extends PublicChatChannelService<CommonPublicChatMessage, CommonPublicChatChannel, CommonPublicChatChannelStore> {
    @Getter
    private final CommonPublicChatChannelStore persistableStore = new CommonPublicChatChannelStore();
    @Getter
    private final Persistence<CommonPublicChatChannelStore> persistence;
    private final List<CommonPublicChatChannel> defaultChannels;

    public CommonPublicChatChannelService(PersistenceService persistenceService,
                                          NetworkService networkService,
                                          UserIdentityService userIdentityService,
                                          UserProfileService userProfileService,
                                          ChatChannelDomain chatChannelDomain,
                                          List<CommonPublicChatChannel> defaultChannels) {
        super(networkService, userIdentityService, userProfileService, chatChannelDomain);

        this.defaultChannels = defaultChannels;

        persistence = persistenceService.getOrCreatePersistence(this,
                "db",
                "Public" + StringUtils.capitalize(chatChannelDomain.name()) + "ChannelStore",
                persistableStore);

        this.defaultChannels.forEach(channel -> channel.getChannelNotificationType().addObserver(value -> persist()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof CommonPublicChatMessage) {
            processAddedMessage((CommonPublicChatMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof CommonPublicChatMessage) {
            processRemovedMessage((CommonPublicChatMessage) distributedData);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PublicChannelService 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableArray<CommonPublicChatChannel> getChannels() {
        return persistableStore.getChannels();
    }

    @Override
    protected CommonPublicChatMessage createChatMessage(String text,
                                                        Optional<Quotation> quotedMessage,
                                                        CommonPublicChatChannel commonPublicChatChannel,
                                                        UserProfile userProfile) {
        return new CommonPublicChatMessage(commonPublicChatChannel.getChatChannelDomain(),
                commonPublicChatChannel.getChannelName(),
                userProfile.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
    }

    @Override
    protected CommonPublicChatMessage createEditedChatMessage(CommonPublicChatMessage originalChatMessage,
                                                              String editedText,
                                                              UserProfile userProfile) {
        return new CommonPublicChatMessage(originalChatMessage.getChatChannelDomain(),
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