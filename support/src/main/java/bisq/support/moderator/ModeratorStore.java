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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
public final class ModeratorStore implements PersistableStore<ModeratorStore> {
    private static final int VERSION = 1;
    @Getter(AccessLevel.PACKAGE)
    private final ObservableSet<ReportToModeratorMessage> reportToModeratorMessages = new ObservableSet<>();
    @Getter(AccessLevel.PACKAGE)
    private final TreeMap<String, BannedUserModeratorData> bannedUserModeratorDataMap = new TreeMap<>();

    ModeratorStore() {
        this(new HashSet<>(), new TreeMap<>());
    }

    private ModeratorStore(Set<ReportToModeratorMessage> reportToModeratorMessages,
                           Map<String, BannedUserModeratorData> bannedUserModeratorDataMap) {
        this.reportToModeratorMessages.addAll(reportToModeratorMessages);
        this.bannedUserModeratorDataMap.putAll(bannedUserModeratorDataMap);
    }

    @Override
    public bisq.support.protobuf.ModeratorStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.ModeratorStore.newBuilder()
                .addAllReportToModeratorMessages(reportToModeratorMessages.stream()
                        .map(e -> e.toValueProto(serializeForHash))
                        .collect(Collectors.toList()))
                .addAllBannedUserModeratorData(bannedUserModeratorDataMap.values().stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.support.protobuf.ModeratorStore toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static ModeratorStore fromProto(bisq.support.protobuf.ModeratorStore proto) {
        Set<ReportToModeratorMessage> messages = proto.getReportToModeratorMessagesList().stream()
                .map(ReportToModeratorMessage::fromProto)
                .collect(Collectors.toSet());

        Map<String, BannedUserModeratorData> bannedDataMap = new TreeMap<>();
        if (proto.getBannedUserModeratorDataCount() > 0) {
            bannedDataMap = proto.getBannedUserModeratorDataList().stream()
                    .map(BannedUserModeratorData::fromProto)
                    .collect(Collectors.toMap(
                            BannedUserModeratorData::getAccusedUserProfileId,
                            data -> data,
                            (existing, replacement) -> replacement,
                            TreeMap::new
                    ));
        }

        return new ModeratorStore(messages, bannedDataMap);
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
        return new ModeratorStore(new HashSet<>(reportToModeratorMessages),
                new TreeMap<>(bannedUserModeratorDataMap));
    }

    @Override
    public void applyPersisted(ModeratorStore persisted) {
        reportToModeratorMessages.setAll(persisted.getReportToModeratorMessages());
        bannedUserModeratorDataMap.clear();
        bannedUserModeratorDataMap.putAll(persisted.getBannedUserModeratorDataMap());
    }

    @Override
    public int getVersion() {
        return VERSION;
    }
}