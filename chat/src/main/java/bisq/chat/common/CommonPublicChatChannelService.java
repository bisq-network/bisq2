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

package bisq.chat.common;

import bisq.chat.ChatChannelDomain;
import bisq.chat.Citation;
import bisq.chat.pub.PublicChatChannelService;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
public final class CommonPublicChatChannelService extends PublicChatChannelService<CommonPublicChatMessage,
        CommonPublicChatChannel, CommonPublicChatChannelStore, CommonPublicChatMessageReaction> {
    @Getter
    private final CommonPublicChatChannelStore persistableStore = new CommonPublicChatChannelStore();
    @Getter
    private final Persistence<CommonPublicChatChannelStore> persistence;
    private final List<CommonPublicChatChannel> channels;

    public CommonPublicChatChannelService(PersistenceService persistenceService,
                                          NetworkService networkService,
                                          UserService userService,
                                          ChatChannelDomain chatChannelDomain,
                                          List<CommonPublicChatChannel> channels) {
        super(networkService, userService, chatChannelDomain);

        this.channels = channels;

        String name = StringUtils.capitalize(StringUtils.snakeCaseToCamelCase(chatChannelDomain.name().toLowerCase()));
        persistence = persistenceService.getOrCreatePersistence(this,
                DbSubDirectory.CACHE,
                "Public" + name + "ChatChannelStore",
                persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        handleAuthenticatedDataAdded(authenticatedData);
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof CommonPublicChatMessage) {
            processRemovedMessage((CommonPublicChatMessage) distributedData);
        } else if (distributedData instanceof CommonPublicChatMessageReaction) {
            processRemovedReaction((CommonPublicChatMessageReaction) distributedData);
        }
    }

    @Override
    protected void handleAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof CommonPublicChatMessage) {
            processAddedMessage((CommonPublicChatMessage) distributedData);
        } else if (distributedData instanceof CommonPublicChatMessageReaction) {
            processAddedReaction((CommonPublicChatMessageReaction) distributedData);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableSet<CommonPublicChatChannel> getChannels() {
        return persistableStore.getChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected CommonPublicChatMessage createChatMessage(String text,
                                                        Optional<Citation> citation,
                                                        CommonPublicChatChannel commonPublicChatChannel,
                                                        UserProfile authorUserProfileId) {
        return new CommonPublicChatMessage(commonPublicChatChannel.getChatChannelDomain(),
                commonPublicChatChannel.getId(),
                authorUserProfileId.getId(),
                text,
                citation,
                new Date().getTime(),
                false);
    }

    @Override
    protected CommonPublicChatMessage createEditedChatMessage(CommonPublicChatMessage originalChatMessage,
                                                              String editedText,
                                                              UserProfile authorUserProfileId) {
        return new CommonPublicChatMessage(originalChatMessage.getChatChannelDomain(),
                originalChatMessage.getChannelId(),
                authorUserProfileId.getId(),
                editedText,
                originalChatMessage.getCitation(),
                originalChatMessage.getDate(),
                true);
    }

    @Override
    protected void maybeAddDefaultChannels() {
        if (!getChannels().isEmpty()) {
            return;
        }

        getChannels().setAll(channels);
        persist();
    }

    @Override
    protected CommonPublicChatMessageReaction createChatMessageReaction(CommonPublicChatMessage message,
                                                                        Reaction reaction,
                                                                        UserIdentity userIdentity) {
        return new CommonPublicChatMessageReaction(
                StringUtils.createUid(),
                userIdentity.getId(),
                message.getChannelId(),
                message.getChatChannelDomain(),
                message.getId(),
                reaction.ordinal(),
                new Date().getTime());
    }
}
