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
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * Minimal AuthorizedDistributedData for storage tests, serialized like TestDistributedData. It names no owner, so its
 * authorized key owns it.
 */
@EqualsAndHashCode
public final class TestAuthorizedDistributedData implements AuthorizedDistributedData {
    // Longer than the time to live of real types, so that entries created before AuthorizedData.LEGACY_OWNER_CUTOFF_DATE
    // do not expire while the tests of the transition rule run
    public static final MetaData META_DATA = new MetaData(TimeUnit.DAYS.toMillis(1000),
            TestAuthorizedDistributedData.class.getSimpleName());

    @Getter
    private final String content;
    @EqualsAndHashCode.Exclude
    private final boolean legacyOwnerAccepted;

    TestAuthorizedDistributedData(String content) {
        this(content, false);
    }

    private TestAuthorizedDistributedData(String content, boolean legacyOwnerAccepted) {
        this.content = content;
        this.legacyOwnerAccepted = legacyOwnerAccepted;
    }

    // Data of a type which oracle nodes of earlier versions published with the key of their network identity
    static TestAuthorizedDistributedData withLegacyOwners(String content) {
        return new TestAuthorizedDistributedData(content, true);
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
    public boolean isDataInvalid() {
        return false;
    }

    @Override
    public boolean isLegacyOwnerAccepted() {
        return legacyOwnerAccepted;
    }
}
