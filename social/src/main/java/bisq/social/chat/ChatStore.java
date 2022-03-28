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

import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ChatStore implements PersistableStore<ChatStore> {
    @Getter
    private final ObservableSet<PrivateChannel> privateChannels = new ObservableSet<>();
    @Getter
    private final ObservableSet<PublicChannel> publicChannels = new ObservableSet<>();
    @Getter
    private final Observable<Channel<? extends ChatMessage>> selectedChannel = new Observable<>();
    @Getter
    private final ObservableSet<String> ignoredChatUserIds = new ObservableSet<>();

    public ChatStore() {
    }

    private ChatStore(Set<PrivateChannel> privateChannels,
                      Set<PublicChannel> publicChannels,
                      Channel<? extends ChatMessage> selectedChannel,
                      Set<String> ignoredChatUserIds) {
        setAll(privateChannels,
                publicChannels,
                selectedChannel,
                ignoredChatUserIds);
    }

    @Override
    public bisq.social.protobuf.ChatStore toProto() {
        return bisq.social.protobuf.ChatStore.newBuilder()
                .addAllPrivateChannels(privateChannels.stream().map(PrivateChannel::toProto).collect(Collectors.toSet()))
                .addAllPublicChannels(publicChannels.stream().map(PublicChannel::toProto).collect(Collectors.toSet()))
                .setSelectedChannel(selectedChannel.get().toProto())
                .addAllIgnoredChatUserIds(ignoredChatUserIds)
                .build();
    }

    public static ChatStore fromProto(bisq.social.protobuf.ChatStore proto) {
        return new ChatStore(proto.getPrivateChannelsList().stream()
                .map(e -> (PrivateChannel) PrivateChannel.fromProto(e))
                .collect(Collectors.toSet()),
                proto.getPublicChannelsList().stream()
                        .map(e -> (PublicChannel) PublicChannel.fromProto(e))
                        .collect(Collectors.toSet()),
                Channel.fromProto(proto.getSelectedChannel()),
                new HashSet<>(proto.getIgnoredChatUserIdsList())
        );
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.social.protobuf.ChatStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(ChatStore chatStore) {
        setAll(chatStore.privateChannels,
                chatStore.publicChannels,
                chatStore.selectedChannel.get(),
                chatStore.ignoredChatUserIds);
    }

    @Override
    public ChatStore getClone() {
        return new ChatStore(privateChannels,
                publicChannels,
                selectedChannel.get(),
                ignoredChatUserIds);
    }

    public void setAll(Set<PrivateChannel> privateChannels,
                       Set<PublicChannel> publicChannels,
                       Channel<? extends ChatMessage> selectedChannel,
                       Set<String> ignoredChatUserIds) {
        this.privateChannels.clear();
        this.privateChannels.addAll(privateChannels);
        this.publicChannels.clear();
        this.publicChannels.addAll(publicChannels);
        this.selectedChannel.set(selectedChannel);
        this.ignoredChatUserIds.clear();
        this.ignoredChatUserIds.addAll(ignoredChatUserIds);
    }

    public Optional<PrivateChannel> findPrivateChannel(String id) {
        return privateChannels.stream().filter(e -> e.getId().equals(id)).findAny();
    }

    public Optional<PublicChannel> findPublicChannel(String id) {
        return publicChannels.stream().filter(e -> e.getId().equals(id)).findAny();
    }
}