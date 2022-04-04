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

package bisq.social.chat;

import bisq.common.observable.ObservableSet;
import bisq.social.user.ChatUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class PublicChannel extends Channel<PublicChatMessage> {
    private final String channelName;
    private final String description;
    private final ChatUser channelAdmin;
    private final Set<ChatUser> channelModerators;
    private final ObservableSet<String> tradeTags = new ObservableSet<>();
    private final ObservableSet<String> currencyTags = new ObservableSet<>();
    private final ObservableSet<String> paymentMethodTags = new ObservableSet<>();
    private final ObservableSet<String> customTags = new ObservableSet<>();

    public PublicChannel(String id,
                         String channelName,
                         String description,
                         ChatUser channelAdmin,
                         Set<ChatUser> channelModerators,
                         Set<String> tradeTags,
                         Set<String> currencyTags,
                         Set<String> paymentMethodTags,
                         Set<String> customTags
    ) {
        this(id, channelName,
                description,
                channelAdmin,
                channelModerators,
                NotificationSetting.MENTION,
                new HashSet<>(),
                tradeTags,
                currencyTags,
                paymentMethodTags,
                customTags
        );
    }

    public PublicChannel(String id,
                         String channelName,
                         String description,
                         ChatUser channelAdmin,
                         Set<ChatUser> channelModerators,
                         NotificationSetting notificationSetting,
                         Set<PublicChatMessage> chatMessages,
                         Set<String> tradeTags,
                         Set<String> currencyTags,
                         Set<String> paymentMethodTags,
                         Set<String> customTags) {
        super(id, notificationSetting, chatMessages);

        this.channelName = channelName;
        this.description = description;
        this.channelAdmin = channelAdmin;
        this.channelModerators = channelModerators;
        this.tradeTags.addAll(tradeTags);
        this.currencyTags.addAll(currencyTags);
        this.paymentMethodTags.addAll(paymentMethodTags);
        this.customTags.addAll(customTags);
    }

    public bisq.social.protobuf.Channel toProto() {
        return getChannelBuilder().setPublicChannel(bisq.social.protobuf.PublicChannel.newBuilder()
                        .setChannelName(channelName)
                        .setDescription(description)
                        .setChannelAdmin(channelAdmin.toProto())
                        .addAllChannelModerators(channelModerators.stream().map(ChatUser::toProto).collect(Collectors.toList()))
                        .addAllTradeTags(tradeTags)
                        .addAllCurrencyTags(currencyTags)
                        .addAllPaymentMethodTags(paymentMethodTags)
                        .addAllCustomTags(customTags)
                )
                .build();
    }

    public static PublicChannel fromProto(bisq.social.protobuf.Channel baseProto,
                                          bisq.social.protobuf.PublicChannel proto) {
        return new PublicChannel(
                baseProto.getId(),
                proto.getChannelName(),
                proto.getDescription(),
                ChatUser.fromProto(proto.getChannelAdmin()),
                proto.getChannelModeratorsList().stream().map(ChatUser::fromProto).collect(Collectors.toSet()),
                NotificationSetting.fromProto(baseProto.getNotificationSetting()),
                baseProto.getChatMessagesList().stream()
                        .map(PublicChatMessage::fromProto)
                        .collect(Collectors.toSet()),
                new HashSet<>(proto.getTradeTagsList()),
                new HashSet<>(proto.getCurrencyTagsList()),
                new HashSet<>(proto.getPaymentMethodTagsList()),
                new HashSet<>(proto.getCustomTagsList())
        );
    }

    @Override
    protected bisq.social.protobuf.ChatMessage getChatMessageProto(PublicChatMessage chatMessage) {
        return chatMessage.toProto();
    }
}