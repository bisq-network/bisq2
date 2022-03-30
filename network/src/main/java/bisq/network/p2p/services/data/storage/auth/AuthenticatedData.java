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

package bisq.network.p2p.services.data.storage.auth;

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorageData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for AuthenticatedData or AuthorizedData. Holds DistributedData.
 * We use Any for wrapping the external implementation of the distributedData instance (e.g. Offer).
 */
@Slf4j
@ToString
@EqualsAndHashCode
public abstract class AuthenticatedData implements StorageData {
    @Getter
    protected final DistributedData distributedData;

    public AuthenticatedData(DistributedData distributedData) {
        this.distributedData = distributedData;
    }

    abstract public bisq.network.protobuf.AuthenticatedData toProto();

    public bisq.network.protobuf.AuthenticatedData.Builder getAuthenticatedDataBuilder() {
        return bisq.network.protobuf.AuthenticatedData.newBuilder()
                .setDistributedData(distributedData.toAny());
    }

    public static AuthenticatedData fromProto(bisq.network.protobuf.AuthenticatedData proto) {
        switch (proto.getMessageCase()) {
            case AUTHENTICATEDDATAIMPL -> {
                return AuthenticatedDataImpl.fromProto(proto);
            }
            case AUTHORIZEDDATA -> {
                return AuthorizedData.fromProto(proto);
            }
            case MESSAGE_NOT_SET -> {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }


    // We delegate the delivery of MetaData to the distributedData.
    @Override
    public MetaData getMetaData() {
        return distributedData.getMetaData();
    }
}
