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
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.security.DigestUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.security.PublicKey;
import java.util.Optional;

/**
 * Minimal AuthorizedDistributedData for storage tests, serialized like TestDistributedData. The owner which the data
 * names is not serialized.
 */
@EqualsAndHashCode
public final class TestAuthorizedDistributedData implements AuthorizedDistributedData {
    public static final MetaData META_DATA = new MetaData(MetaData.TTL_10_DAYS,
            TestAuthorizedDistributedData.class.getSimpleName());

    @Getter
    private final String content;
    @EqualsAndHashCode.Exclude
    private final Optional<byte[]> ownerPubKeyHash;
    @EqualsAndHashCode.Exclude
    private final boolean legacyOwnerAccepted;

    // Data which its authorized key owns
    TestAuthorizedDistributedData(String content) {
        this(content, Optional.empty(), false);
    }

    private TestAuthorizedDistributedData(String content, Optional<byte[]> ownerPubKeyHash, boolean legacyOwnerAccepted) {
        this.content = content;
        this.ownerPubKeyHash = ownerPubKeyHash;
        this.legacyOwnerAccepted = legacyOwnerAccepted;
    }

    // Data which names its owner, like AuthorizedOracleNode
    static TestAuthorizedDistributedData withOwner(String content, PublicKey owner) {
        return new TestAuthorizedDistributedData(content, Optional.of(DigestUtil.hash(owner.getEncoded())), false);
    }

    // Data of a type which oracle nodes of earlier versions published with the key of their network identity
    static TestAuthorizedDistributedData withLegacyOwners(String content) {
        return new TestAuthorizedDistributedData(content, Optional.empty(), true);
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
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }

    @Override
    public Optional<byte[]> findOwnerPubKeyHash() {
        return ownerPubKeyHash;
    }

    @Override
    public boolean isLegacyOwnerAccepted() {
        return legacyOwnerAccepted;
    }
}
