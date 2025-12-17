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

package bisq.account.age_witness;

import bisq.common.annotation.ExcludeForHash;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static bisq.network.p2p.services.data.storage.MetaData.TTL_30_DAYS;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public final class AccountAgeWitness implements DistributedData {
    private transient final MetaData metaData = new MetaData(TTL_30_DAYS, getClass().getSimpleName());

    private final byte[] hash;

    // We exclude the date so that the hash is the only input for the hash in the storage map. This ensures that only
    // one entry can exist for a given AccountAgeWitness and changes of date would be ignored.
    @ExcludeForHash
    private final long date;

    public AccountAgeWitness(byte[] hash, long date) {
        this.hash = hash;
        this.date = date;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(hash);
    }

    @Override
    public bisq.account.protobuf.AccountAgeWitness.Builder getBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AccountAgeWitness.newBuilder()
                .setHash(ByteString.copyFrom(hash))
                .setDate(date);
    }

    @Override
    public bisq.account.protobuf.AccountAgeWitness toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static AccountAgeWitness fromProto(bisq.account.protobuf.AccountAgeWitness proto) {
        return new AccountAgeWitness(proto.getHash().toByteArray(), proto.getDate());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return AccountAgeWitness.fromProto(any.unpack(bisq.account.protobuf.AccountAgeWitness.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return 0.2;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }
}