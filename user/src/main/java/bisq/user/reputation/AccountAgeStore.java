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

package bisq.user.reputation;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Getter
public final class AccountAgeStore implements PersistableStore<AccountAgeStore> {
    private final Set<String> jsonRequests = new CopyOnWriteArraySet<>();
    @Setter
    private long lastRequested = 0;

    public AccountAgeStore() {
    }

    private AccountAgeStore(Set<String> jsonRequests, long lastRequested) {
        this.lastRequested = lastRequested;
        this.jsonRequests.addAll(jsonRequests);
    }

    @Override
    public bisq.user.protobuf.AccountAgeStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.AccountAgeStore.newBuilder()
                .addAllJsonRequests(jsonRequests)
                .setLastRequested(lastRequested);
    }

    @Override
    public bisq.user.protobuf.AccountAgeStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AccountAgeStore fromProto(bisq.user.protobuf.AccountAgeStore proto) {
        return new AccountAgeStore(new HashSet<>(proto.getJsonRequestsList()), proto.getLastRequested());
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.AccountAgeStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public AccountAgeStore getClone() {
        return new AccountAgeStore(new HashSet<>(jsonRequests), lastRequested);
    }

    @Override
    public void applyPersisted(AccountAgeStore persisted) {
        jsonRequests.clear();
        jsonRequests.addAll(persisted.getJsonRequests());
        lastRequested = persisted.getLastRequested();
    }
}