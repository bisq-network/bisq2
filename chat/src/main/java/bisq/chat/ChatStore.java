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

package bisq.chat;

import bisq.chat.channels.*;
import bisq.chat.messages.ChatMessage;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
public final class ChatStore implements PersistableStore<ChatStore> {
    private final ObservableSet<PublicDiscussionChannel> publicDiscussionChannels = new ObservableSet<>();
    private final ObservableSet<PublicTradeChannel> publicTradeChannels = new ObservableSet<>();
    private final Observable<Channel<? extends ChatMessage>> selectedTradeChannel = new Observable<>();
    private final Observable<Channel<? extends ChatMessage>> selectedDiscussionChannel = new Observable<>();
    private final ObservableSet<String> customTags = new ObservableSet<>();
    private final ObservableSet<String> ignoredChatUserIds = new ObservableSet<>();

    public ChatStore() {
    }

    private ChatStore(
                      Set<PublicDiscussionChannel> publicDiscussionChannels,
                      Set<PublicTradeChannel> publicTradeChannels,
                      Channel<? extends ChatMessage> selectedTradeChannel,
                      Channel<? extends ChatMessage> selectedDiscussionChannel,
                      Set<String> customTags,
                      Set<String> ignoredChatUserIds) {
        setAll(
                publicDiscussionChannels,
                publicTradeChannels,
                selectedTradeChannel,
                selectedDiscussionChannel,
                customTags,
                ignoredChatUserIds);
    }

    @Override
    public bisq.chat.protobuf.ChatStore toProto() {
        return bisq.chat.protobuf.ChatStore.newBuilder()
                .addAllPublicDiscussionChannels(publicDiscussionChannels.stream().map(PublicDiscussionChannel::toProto).collect(Collectors.toSet()))
                .addAllPublicTradeChannels(publicTradeChannels.stream().map(PublicTradeChannel::toProto).collect(Collectors.toSet()))
                .setSelectedTradeChannel(selectedTradeChannel.get().toProto())
                .setSelectedDiscussionChannel(selectedDiscussionChannel.get().toProto())
                .addAllCustomTags(customTags)
                .addAllIgnoredChatUserProfileIds(ignoredChatUserIds)
                .build();
    }

    public static ChatStore fromProto(bisq.chat.protobuf.ChatStore proto) {
        Set<PrivateDiscussionChannel> privateDiscussionChannels = proto.getPrivateDiscussionChannelsList().stream()
                .map(e -> (PrivateDiscussionChannel) PrivateDiscussionChannel.fromProto(e))
                .collect(Collectors.toSet());
        Set<PublicDiscussionChannel> publicDiscussionChannels = proto.getPublicDiscussionChannelsList().stream()
                .map(e -> (PublicDiscussionChannel) PublicDiscussionChannel.fromProto(e))
                .collect(Collectors.toSet());
        Set<PublicTradeChannel> publicTradeChannels = proto.getPublicTradeChannelsList().stream()
                .map(e -> (PublicTradeChannel) PublicTradeChannel.fromProto(e))
                .collect(Collectors.toSet());
        return new ChatStore(
                publicDiscussionChannels,
                publicTradeChannels,
                Channel.fromProto(proto.getSelectedTradeChannel()),
                Channel.fromProto(proto.getSelectedDiscussionChannel()),
                new HashSet<>(proto.getCustomTagsCount()),
                new HashSet<>(proto.getIgnoredChatUserProfileIdsList())
        );
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.ChatStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(ChatStore chatStore) {
        setAll(chatStore.publicDiscussionChannels,
                chatStore.publicTradeChannels,
                chatStore.selectedTradeChannel.get(),
                chatStore.selectedDiscussionChannel.get(),
                chatStore.getCustomTags(),
                chatStore.ignoredChatUserIds);
    }

    @Override
    public ChatStore getClone() {
        return new ChatStore(
                publicDiscussionChannels,
                publicTradeChannels,
                selectedTradeChannel.get(),
                selectedDiscussionChannel.get(),
                customTags,
                ignoredChatUserIds);
    }

    public void setAll(
                       Set<PublicDiscussionChannel> publicDiscussionChannels,
                       Set<PublicTradeChannel> publicTradeChannels,
                       Channel<? extends ChatMessage> selectedTradeChannel,
                       Channel<? extends ChatMessage> selectedDiscussionChannel,
                       Set<String> customTags,
                       Set<String> ignoredChatUserIds) {
        this.publicDiscussionChannels.clear();
        this.publicDiscussionChannels.addAll(publicDiscussionChannels);
        this.publicTradeChannels.clear();
        this.publicTradeChannels.addAll(publicTradeChannels);
        this.selectedTradeChannel.set(selectedTradeChannel);
        this.selectedDiscussionChannel.set(selectedDiscussionChannel);
        this.customTags.clear();
        this.customTags.addAll(customTags);
        this.ignoredChatUserIds.clear();
        this.ignoredChatUserIds.addAll(ignoredChatUserIds);
    }
}