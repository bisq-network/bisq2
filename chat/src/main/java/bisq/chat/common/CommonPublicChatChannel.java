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
import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.chat.pub.PublicChatChannel;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class CommonPublicChatChannel extends PublicChatChannel<CommonPublicChatMessage> {
    private final Optional<String> channelAdminId;
    private final List<String> channelModeratorIds;
    private final String channelTitle;
    private transient final SubDomain subDomain;

    public CommonPublicChatChannel(ChatChannelDomain chatChannelDomain, SubDomain subDomain) {
        this(subDomain.getChannelId(),
                subDomain.getChatChannelDomain(),
                subDomain.getTitle(),
                Optional.empty(),
                new ArrayList<>(),
                ChatChannelNotificationType.GLOBAL_DEFAULT);
    }

    private CommonPublicChatChannel(String id,
                                    ChatChannelDomain chatChannelDomain,
                                    String channelTitle,
                                    Optional<String> channelAdminId,
                                    List<String> channelModeratorIds,
                                    ChatChannelNotificationType chatChannelNotificationType) {
        super(id, chatChannelDomain, chatChannelNotificationType);

        this.channelTitle = channelTitle;
        this.channelAdminId = channelAdminId;
        this.channelModeratorIds = channelModeratorIds;
        // We need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.channelModeratorIds);

        this.subDomain = SubDomain.from(chatChannelDomain, channelTitle);
    }

    @Override
    public bisq.chat.protobuf.ChatChannel.Builder getBuilder(boolean serializeForHash) {
        bisq.chat.protobuf.CommonPublicChatChannel.Builder builder = bisq.chat.protobuf.CommonPublicChatChannel.newBuilder()
                .setChannelTitle(channelTitle)
                .addAllChannelModeratorIds(channelModeratorIds);
        channelAdminId.ifPresent(builder::setChannelAdminId);
        return getChatChannelBuilder().setCommonPublicChatChannel(builder);
    }

    public static CommonPublicChatChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                                    bisq.chat.protobuf.CommonPublicChatChannel proto) {
        return new CommonPublicChatChannel(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                proto.getChannelTitle(),
                proto.hasChannelAdminId() ? Optional.of(proto.getChannelAdminId()) : Optional.empty(),
                new ArrayList<>(proto.getChannelModeratorIdsList()),
                ChatChannelNotificationType.fromProto(baseProto.getChatChannelNotificationType()));
    }

    @Override
    public ChatChannelDomain getChatChannelDomain() {
        return chatChannelDomain.migrate();
    }

    @Override
    public String getId() {
        return Migration.migrateChannelId(id);
    }

    @Override
    public String getDisplayString() {
        return Res.get(chatChannelDomain.name().toLowerCase() + "." + getChannelTitle() + ".title");
    }

    public String getDescription() {
        return Res.get(chatChannelDomain.name().toLowerCase() + "." + getChannelTitle() + ".description");
    }


    @Slf4j
    public static class Migration {
        public static CommonPublicChatChannel migrate(CommonPublicChatChannel channel) {
            SubDomain subDomain = channel.getSubDomain().migrate();
            return new CommonPublicChatChannel(subDomain.getChannelId(),
                    subDomain.getChatChannelDomain(),
                    subDomain.getTitle(),
                    channel.getChannelAdminId(),
                    channel.getChannelModeratorIds(),
                    channel.getChatChannelNotificationType().get());
        }

        public static String migrateChannelId(ChatChannelDomain chatChannelDomain, String channelTitle) {
            return SubDomain.from(chatChannelDomain, channelTitle).migrate().getChannelId();
        }

        public static String migrateChannelId(String channelId) {
            return SubDomain.from(channelId).migrate().getChannelId();
        }

    }
}