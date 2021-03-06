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

package bisq.oracle.daobridge.model;

import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle.daobridge.dto.ProofOfBurnDto;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedProofOfBurnData implements AuthorizedDistributedData {
    // The pubKeys which are authorized for publishing that data.
    // todo Production key not set yet - we use devMode key only yet
    private static final Set<String> authorizedPublicKeys = Set.of();

    private final MetaData metaData = new MetaData(TimeUnit.DAYS.toMillis(1),
            100000,
            AuthorizedProofOfBurnData.class.getSimpleName());

    private final long amount;
    private final long time;
    private final byte[] hash;

    public static AuthorizedProofOfBurnData from(ProofOfBurnDto dto) {
        return new AuthorizedProofOfBurnData(
                dto.getAmount(),
                dto.getTime(),
                Hex.decode(dto.getHash()));
    }

    public AuthorizedProofOfBurnData(long amount, long time, byte[] hash) {
        this.amount = amount;
        this.time = time;
        this.hash = hash;
    }

    @Override
    public bisq.oracle.protobuf.AuthorizedProofOfBurnData toProto() {
        return bisq.oracle.protobuf.AuthorizedProofOfBurnData.newBuilder()
                .setAmount(amount)
                .setTime(time)
                .setHash(ByteString.copyFrom(hash))
                .build();
    }

    public static AuthorizedProofOfBurnData fromProto(bisq.oracle.protobuf.AuthorizedProofOfBurnData proto) {
        return new AuthorizedProofOfBurnData(
                proto.getAmount(),
                proto.getTime(),
                proto.getHash().toByteArray());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.oracle.protobuf.AuthorizedProofOfBurnData.class));
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

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return Set.of(
                    "3056301006072a8648ce3d020106052b8104000a03420004170a828efbaa0316b7a59ec5a1e8033ca4c215b5e58b17b16f3e3cbfa5ec085f4bdb660c7b766ec5ba92b432265ba3ed3689c5d87118fbebe19e92b9228aca63"
            );
        } else {
            return authorizedPublicKeys;
        }
    }

    @Override
    public String toString() {
        return "AuthorizedProofOfBurnData{" +
                ",\r\n     amount=" + amount +
                ",\r\n     time=" + new Date(time) +
                ",\r\n     hash=" + Hex.encode(hash) +
                "\r\n}";
    }
}