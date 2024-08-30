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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class DefaultAuthenticatedData extends AuthenticatedData {
    public DefaultAuthenticatedData(DistributedData distributedData) {
        super(distributedData);

        verify();
    }

    @Override
    public void verify() {
    }

    public bisq.network.protobuf.AuthenticatedData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.AuthenticatedData.Builder getBuilder(boolean serializeForHash) {
        return getAuthenticatedDataBuilder(serializeForHash).setDefaultAuthenticatedData(
                bisq.network.protobuf.DefaultAuthenticatedData.newBuilder());
    }

    public static DefaultAuthenticatedData fromProto(bisq.network.protobuf.AuthenticatedData proto) {
        return new DefaultAuthenticatedData(DistributedData.fromAny(proto.getDistributedData()));
    }

    @Override
    public boolean isDataInvalid(byte[] ownerPubKeyHash) {
        return distributedData.isDataInvalid(ownerPubKeyHash);
    }
}
