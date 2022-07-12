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

import bisq.chat.channel.Channel;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Observable;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public final class ChatStore implements PersistableStore<ChatStore> {
    private final Observable<Channel<? extends ChatMessage>> selectedTradeChannel = new Observable<>();
    private final Observable<Channel<? extends ChatMessage>> selectedDiscussionChannel = new Observable<>();

    public ChatStore() {
    }

    private ChatStore(Channel<? extends ChatMessage> selectedTradeChannel,
                      Channel<? extends ChatMessage> selectedDiscussionChannel) {
        setAll(selectedTradeChannel, selectedDiscussionChannel);
    }

    @Override
    public bisq.chat.protobuf.ChatStore toProto() {
        return bisq.chat.protobuf.ChatStore.newBuilder()
                .setSelectedTradeChannel(selectedTradeChannel.get().toProto())
                .setSelectedDiscussionChannel(selectedDiscussionChannel.get().toProto())
                .build();
    }

    public static ChatStore fromProto(bisq.chat.protobuf.ChatStore proto) {
        return new ChatStore(Channel.fromProto(proto.getSelectedTradeChannel()),
                Channel.fromProto(proto.getSelectedDiscussionChannel()));
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
        setAll(chatStore.selectedTradeChannel.get(), chatStore.selectedDiscussionChannel.get());
    }

    @Override
    public ChatStore getClone() {
        return new ChatStore(selectedTradeChannel.get(), selectedDiscussionChannel.get());
    }

    public void setAll(Channel<? extends ChatMessage> selectedTradeChannel,
                       Channel<? extends ChatMessage> selectedDiscussionChannel) {
        this.selectedTradeChannel.set(selectedTradeChannel);
        this.selectedDiscussionChannel.set(selectedDiscussionChannel);
    }
}