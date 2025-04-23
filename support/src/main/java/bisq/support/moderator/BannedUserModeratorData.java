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

import bisq.common.proto.PersistableProto;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class BannedUserModeratorData implements PersistableProto {
    private final String reporterUserProfileId;
    private final String accusedUserProfileId;
    private final String reportersMessage;
    private final String banReason;

    public BannedUserModeratorData(String reporterUserProfileId,
                                   String accusedUserProfileId,
                                   String reportersMessage,
                                   String banReason) {
        this.reporterUserProfileId = reporterUserProfileId;
        this.accusedUserProfileId = accusedUserProfileId;
        this.reportersMessage = reportersMessage;
        this.banReason = banReason;
    }

    @Override
    public bisq.support.protobuf.BannedUserModeratorData.Builder getBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.BannedUserModeratorData.newBuilder()
                .setReporterUserProfileId(reporterUserProfileId)
                .setAccusedUserProfileId(accusedUserProfileId)
                .setReportMessage(reportersMessage)
                .setBanReason(banReason);
    }

    @Override
    public bisq.support.protobuf.BannedUserModeratorData toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static BannedUserModeratorData fromProto(bisq.support.protobuf.BannedUserModeratorData proto) {
        return new BannedUserModeratorData(
                proto.getReporterUserProfileId(),
                proto.getAccusedUserProfileId(),
                proto.getReportMessage(),
                proto.getBanReason());
    }

    public static ProtoResolver<PersistableProto> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.support.protobuf.BannedUserModeratorData.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }
}