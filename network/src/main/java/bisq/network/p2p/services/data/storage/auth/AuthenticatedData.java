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

import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorageData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Container for DistributedData.
 * We use Any for wrapping the external implementation of the distributedData instance (e.g. Offer).
 */
@ToString
@EqualsAndHashCode
public class AuthenticatedData implements StorageData {
    @Getter
    protected final DistributedData distributedData;

    public AuthenticatedData(DistributedData distributedData) {
        this.distributedData = distributedData;
    }

    public bisq.network.protobuf.AuthenticatedData toProto() {
        return bisq.network.protobuf.AuthenticatedData.newBuilder()
                .setDistributedData(distributedData.toAny())
                .build();
    }

    public static AuthenticatedData fromProto(bisq.network.protobuf.AuthenticatedData proto) {
        return new AuthenticatedData(DistributedData.resolve(proto.getDistributedData()));
    }

    // We delegate the delivery of MetaData to the distributedData.
    @Override
    public MetaData getMetaData() {
        return distributedData.getMetaData();
    }

    @Override
    public boolean isDataInvalid() {
        return distributedData.isDataInvalid();
    }
}
