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
public final class AuthorizeAccountTimestampV1Payload implements NetworkProto {
    public final static int MAX_FINGERPRINT_LENGTH = 1000;
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);

    private final long date;
    private final byte[] hash;
    private final byte[] fingerprint;
    private final byte[] saltedFingerprint;
    private final byte[] publicKey;     // Account pub key (DSA key)

    public AuthorizeAccountTimestampV1Payload(long date,
                                              byte[] hash,
                                              byte[] fingerprint,
                                              byte[] saltedFingerprint,
                                              byte[] publicKey) {
        this.date = date;
        this.hash = hash;
        this.fingerprint = fingerprint;
        this.saltedFingerprint = saltedFingerprint;
        this.publicKey = publicKey;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateDate(date);
        NetworkDataValidation.validateHash(hash);

        // fingerprint is created by appending account data, thus no clear boundaries, though 1000 should cover all cases
        NetworkDataValidation.validateByteArray(fingerprint, 1, MAX_FINGERPRINT_LENGTH);

        // We appended 32 byte salt
        NetworkDataValidation.validateByteArray(saltedFingerprint, 1, MAX_FINGERPRINT_LENGTH + 32);

        // Usually around 440–460 bytes
        NetworkDataValidation.validateByteArray(publicKey, 300, 600);
    }

    @Override
    public bisq.account.protobuf.AuthorizeAccountTimestampV1Payload.Builder getBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AuthorizeAccountTimestampV1Payload.newBuilder()
                .setDate(date)
                .setHash(ByteString.copyFrom(hash))
                .setFingerprint(ByteString.copyFrom(fingerprint))
                .setSaltedFingerprint(ByteString.copyFrom(saltedFingerprint))
                .setPublicKey(ByteString.copyFrom(publicKey));
    }

    public static AuthorizeAccountTimestampV1Payload fromProto(bisq.account.protobuf.AuthorizeAccountTimestampV1Payload proto) {
        return new AuthorizeAccountTimestampV1Payload(
                proto.getDate(),
                proto.getHash().toByteArray(),
                proto.getFingerprint().toByteArray(),
                proto.getSaltedFingerprint().toByteArray(),
                proto.getPublicKey().toByteArray()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthorizeAccountTimestampV1Payload that)) return false;

        return date == that.date &&
                Arrays.equals(hash, that.hash) &&
                Arrays.equals(fingerprint, that.fingerprint) &&
                Arrays.equals(saltedFingerprint, that.saltedFingerprint) &&
                Arrays.equals(publicKey, that.publicKey);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(date);
        result = 31 * result + Arrays.hashCode(hash);
        result = 31 * result + Arrays.hashCode(fingerprint);
        result = 31 * result + Arrays.hashCode(saltedFingerprint);
        result = 31 * result + Arrays.hashCode(publicKey);
        return result;
    }
}
