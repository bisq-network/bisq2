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

package bisq.account.accountage;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

@Getter
@ToString
@EqualsAndHashCode
public class AccountAgeWitnessData implements AppendOnlyData {
    private final byte[] hash;                      // Ripemd160(Sha256(concatenated accountHash, signature and sigPubKey)); 20 bytes
    private final long date;                        // 8 byte

    private final MetaData metaData = new MetaData(TimeUnit.DAYS.toMillis(300),
            100000,
            AccountAgeWitnessData.class.getSimpleName());

    public AccountAgeWitnessData(byte[] hash, long date) {
        this.hash = hash;
        this.date = date;
    }

    public bisq.account.protobuf.AccountAgeWitnessData toProto() {
        return bisq.account.protobuf.AccountAgeWitnessData.newBuilder()
                .setHash(ByteString.copyFrom(hash))
                .setDate(date)
                .build();
    }

    public static AccountAgeWitnessData fromProto(bisq.account.protobuf.AccountAgeWitnessData proto) {
        return new AccountAgeWitnessData(proto.getHash().toByteArray(), proto.getDate());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.account.protobuf.AccountAgeWitnessData.class));
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
    public boolean isDataInvalid() {
        return false;
    }
}