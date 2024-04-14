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

package bisq.support.moderator;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class ModeratorStore implements PersistableStore<ModeratorStore> {
    private final ObservableSet<ReportToModeratorMessage> reportToModeratorMessages = new ObservableSet<>();

    public ModeratorStore() {
        this(new HashSet<>());
    }

    private ModeratorStore(Set<ReportToModeratorMessage> reportToModeratorMessages) {
        this.reportToModeratorMessages.addAll(reportToModeratorMessages);
    }

    @Override
    public bisq.support.protobuf.ModeratorStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.ModeratorStore.newBuilder()
                .addAllReportToModeratorMessages(reportToModeratorMessages.stream()
                        .map(e -> e.toValueProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.support.protobuf.ModeratorStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static ModeratorStore fromProto(bisq.support.protobuf.ModeratorStore proto) {
        return new ModeratorStore(proto.getReportToModeratorMessagesList().stream()
                .map(ReportToModeratorMessage::fromProto)
                .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.support.protobuf.ModeratorStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public ModeratorStore getClone() {
        return new ModeratorStore(new HashSet<>(reportToModeratorMessages));
    }

    @Override
    public void applyPersisted(ModeratorStore persisted) {
        reportToModeratorMessages.setAll(persisted.getReportToModeratorMessages());
    }

    ObservableSet<ReportToModeratorMessage> getReportToModeratorMessages() {
        return reportToModeratorMessages;
    }
}