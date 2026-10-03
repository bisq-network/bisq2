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

import bisq.common.encoding.Hex;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.OwnershipVerifiableData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.security.DigestUtil;

import java.security.PublicKey;
import java.util.Set;

/**
 * Authorized data which names its owner, like AuthorizedOracleNode
 */
public final class TestAuthorizedDataWithOwner implements AuthorizedDistributedData, OwnershipVerifiableData {
    public static final MetaData META_DATA = new MetaData(MetaData.TTL_10_DAYS,
            TestAuthorizedDataWithOwner.class.getSimpleName());

    private final byte[] ownerPubKeyHash;
    // A real type has its static keys in the code. The tests pass them, because they use generated key pairs.
    private final Set<String> staticPublicKeys;

    public TestAuthorizedDataWithOwner(PublicKey owner) {
        this(owner, Set.of());
    }

    public TestAuthorizedDataWithOwner(PublicKey owner, Set<String> staticPublicKeys) {
        ownerPubKeyHash = DigestUtil.hash(owner.getEncoded());
        this.staticPublicKeys = staticPublicKeys;
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.MetaData.newBuilder().setClassName(Hex.encode(ownerPubKeyHash));
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

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        return staticPublicKeys;
    }
}
