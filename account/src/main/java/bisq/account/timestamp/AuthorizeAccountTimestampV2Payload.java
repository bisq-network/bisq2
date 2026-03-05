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

package bisq.account.timestamp;

import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.MetaData;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_100;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
public final class AuthorizeAccountTimestampV2Payload implements NetworkProto {
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);

    private final long date;
    private final byte[] hash;
    private final byte[] fingerprintHash; // 20 byte hash
    private final byte[] saltedFingerprintHash; // 20 byte hash
    private final byte[] publicKey;     // Account pub key (EC key)

    public AuthorizeAccountTimestampV2Payload(long date,
                                              byte[] hash,
                                              byte[] fingerprintHash,
                                              byte[] saltedFingerprintHash,
                                              byte[] publicKey) {
        this.date = date;
        this.hash = hash;
        this.fingerprintHash = fingerprintHash;
        this.saltedFingerprintHash = saltedFingerprintHash;
        this.publicKey = publicKey;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateDate(date);
        NetworkDataValidation.validateHash(hash);
        NetworkDataValidation.validateHash(fingerprintHash); 
        NetworkDataValidation.validateHash(saltedFingerprintHash);
        NetworkDataValidation.validateECPubKey(publicKey);
    }

    @Override
    public bisq.account.protobuf.AuthorizeAccountTimestampV2Payload.Builder getBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AuthorizeAccountTimestampV2Payload.newBuilder()
                .setDate(date)
                .setHash(ByteString.copyFrom(hash))
                .setFingerprintHash(ByteString.copyFrom(fingerprintHash))
                .setSaltedFingerprintHash(ByteString.copyFrom(saltedFingerprintHash))
                .setPublicKey(ByteString.copyFrom(publicKey));
    }

    public static AuthorizeAccountTimestampV2Payload fromProto(bisq.account.protobuf.AuthorizeAccountTimestampV2Payload proto) {
        return new AuthorizeAccountTimestampV2Payload(
                proto.getDate(),
                proto.getHash().toByteArray(),
                proto.getFingerprintHash().toByteArray(),
                proto.getSaltedFingerprintHash().toByteArray(),
                proto.getPublicKey().toByteArray()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthorizeAccountTimestampV2Payload that)) return false;

        return date == that.date &&
                Arrays.equals(hash, that.hash) &&
                Arrays.equals(fingerprintHash, that.fingerprintHash) &&
                Arrays.equals(saltedFingerprintHash, that.saltedFingerprintHash) &&
                Arrays.equals(publicKey, that.publicKey);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(date);
        result = 31 * result + Arrays.hashCode(hash);
        result = 31 * result + Arrays.hashCode(fingerprintHash);
        result = 31 * result + Arrays.hashCode(saltedFingerprintHash);
        result = 31 * result + Arrays.hashCode(publicKey);
        return result;
    }
}
