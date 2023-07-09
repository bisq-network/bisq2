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

package bisq.support.alert;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.DeferredAuthorizedPublicKeyValidation;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedAlertData implements AuthorizedDistributedData, DeferredAuthorizedPublicKeyValidation {
    public final static int MAX_MESSAGE_LENGTH = 1000;
    public final static long TTL = TimeUnit.DAYS.toMillis(15);

    private final MetaData metaData = new MetaData(TTL,
            100000,
            AuthorizedAlertData.class.getSimpleName());

    private final String id;
    private final String message;
    private final long date;
    private final AlertType alertType;

    public AuthorizedAlertData(String id, String message, long date, AlertType alertType) {
        this.id = id;
        this.message = message;
        this.date = date;
        this.alertType = alertType;
    }

    @Override
    public bisq.support.protobuf.AuthorizedAlertData toProto() {
        return bisq.support.protobuf.AuthorizedAlertData.newBuilder()
                .setId(id)
                .setMessage(message)
                .setDate(date)
                .setAlertType(alertType.toProto())
                .build();
    }

    public static AuthorizedAlertData fromProto(bisq.support.protobuf.AuthorizedAlertData proto) {
        return new AuthorizedAlertData(
                proto.getId(),
                proto.getMessage(),
                proto.getDate(),
                AlertType.fromProto(proto.getAlertType()));
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.support.protobuf.AuthorizedAlertData.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return message.length() > MAX_MESSAGE_LENGTH;
    }

    @Override
    public String toString() {
        return "AuthorizedAlertData{" +
                ",\r\n     id='" + id + '\'' +
                ",\r\n     message='" + message + '\'' +
                ",\r\n     date=" + date +
                ",\r\n     alertType=" + alertType +
                "\r\n}";
    }
}