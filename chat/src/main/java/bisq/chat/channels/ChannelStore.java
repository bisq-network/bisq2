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

package bisq.chat.channels;

import bisq.chat.messages.ChatMessage;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.persistence.PersistableStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
public abstract class ChannelStore<T extends Channel<?>> implements PersistableStore<ChannelStore<T>> {
    protected final ObservableSet<T> channels = new ObservableSet<>();
    protected final Observable<Channel<? extends ChatMessage>> selected = new Observable<>();

    public ChannelStore() {
    }

    protected ChannelStore(Set<T> channels, Channel<? extends ChatMessage> selected) {
        setAll(channels, selected);
    }

    @Override
    public bisq.chat.protobuf.ChatStore toProto() {
        return bisq.chat.protobuf.ChatStore.newBuilder()
                .addAllPrivateTradeChannels(channels.stream().map(e -> e.toProto()).collect(Collectors.toSet()))
                .setSelectedTradeChannel(selected.get().toProto())
                .build();
    }

    @Override
    public void applyPersisted(ChannelStore<T> chatStore) {
        setAll(chatStore.getChannels(), chatStore.getSelected().get());
    }

    public void setAll(Set<T> channels, Channel<? extends ChatMessage> selected) {
        this.channels.clear();
        this.channels.addAll(channels);
        this.selected.set(selected);
    }
}