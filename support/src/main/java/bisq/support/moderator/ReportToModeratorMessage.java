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

import bisq.chat.ChatChannelDomain;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class ReportToModeratorMessage implements MailboxMessage {
    public final static int MAX_MESSAGE_LENGTH = 1000;

    private final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName());
    private final long date;
    private final String reporterUserProfileId;
    private final UserProfile accusedUserProfile;
    private final String message;
    private final ChatChannelDomain chatChannelDomain;

    public ReportToModeratorMessage(long date,
                                    String reporterUserProfileId,
                                    UserProfile accusedUserProfile,
                                    String message,
                                    ChatChannelDomain chatChannelDomain) {
        this.date = date;
        this.reporterUserProfileId = reporterUserProfileId;
        this.accusedUserProfile = accusedUserProfile;
        this.message = message;
        this.chatChannelDomain = chatChannelDomain;

        NetworkDataValidation.validateDate(date);
        NetworkDataValidation.validateProfileId(reporterUserProfileId);
        NetworkDataValidation.validateText(message, MAX_MESSAGE_LENGTH);

        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize());//1438
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder()
                        .setAny(Any.pack(toReportToModeratorMessageProto())))
                .build();
    }

    public bisq.support.protobuf.ReportToModeratorMessage toReportToModeratorMessageProto() {
        bisq.support.protobuf.ReportToModeratorMessage.Builder builder = bisq.support.protobuf.ReportToModeratorMessage.newBuilder()
                .setDate(date)
                .setReporterUserProfileId(reporterUserProfileId)
                .setAccusedUserProfile(accusedUserProfile.toProto())
                .setMessage(message)
                .setChatChannelDomain(chatChannelDomain.toProto());
        return builder.build();
    }

    public static ReportToModeratorMessage fromProto(bisq.support.protobuf.ReportToModeratorMessage proto) {
        return new ReportToModeratorMessage(proto.getDate(),
                proto.getReporterUserProfileId(),
                UserProfile.fromProto(proto.getAccusedUserProfile()),
                proto.getMessage(),
                ChatChannelDomain.fromProto(proto.getChatChannelDomain()));
    }

    public static ProtoResolver<bisq.network.p2p.message.NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.ReportToModeratorMessage proto = any.unpack(bisq.support.protobuf.ReportToModeratorMessage.class);
                return ReportToModeratorMessage.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }
}