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

import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.OwnershipVerifiableData;
import bisq.security.DigestUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.security.PublicKey;

/**
 * Minimal DistributedData for storage tests. The content is serialized as a MetaData protobuf message, which is
 * sufficient because the tests do not deserialize the payload. The owner is not serialized, so data of different
 * owners can have the same data hash.
 */
@Getter
@EqualsAndHashCode
public final class TestDistributedData implements OwnershipVerifiableData {
    public static final MetaData META_DATA = new MetaData(MetaData.TTL_10_DAYS, TestDistributedData.class.getSimpleName());

    private final String content;
    @EqualsAndHashCode.Exclude
    private final byte[] ownerPubKeyHash;

    public TestDistributedData(String content, PublicKey owner) {
        this.content = content;
        this.ownerPubKeyHash = DigestUtil.hash(owner.getEncoded());
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.MetaData.newBuilder().setClassName(content);
    }

    @Override
    public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public MetaData getMetaData() {
        return META_DATA;
    }

    @Override
    public byte[] getOwnerPubKeyHash() {
        return ownerPubKeyHash;
    }
}
