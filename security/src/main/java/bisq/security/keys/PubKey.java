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

package bisq.security.keys;

import bisq.common.encoding.Hex;
import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.security.DigestUtil;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

@Slf4j
@EqualsAndHashCode
@ToString
public final class PubKey implements NetworkProto {
    // The PublicKey implementation (BCECPublicKey) has an equals and hashCode method implemented.
    // Therefor we can include it for @EqualsAndHashCode.
    @Getter
    @ToString.Exclude
    private final PublicKey publicKey;
    @Getter
    private final String keyId;

    @ToString.Exclude
    private transient byte[] hash;
    private transient String id;

    public PubKey(PublicKey publicKey, String keyId) {
        this.publicKey = publicKey;
        this.keyId = keyId;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateECPubKey(publicKey);
        NetworkDataValidation.validateId(keyId);
    }

    @Override
    public bisq.security.protobuf.PubKey toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.security.protobuf.PubKey.Builder getBuilder(boolean serializeForHash) {
        return bisq.security.protobuf.PubKey.newBuilder()
                .setPublicKey(ByteString.copyFrom(publicKey.getEncoded()))
                .setKeyId(keyId);
    }

    public static PubKey fromProto(bisq.security.protobuf.PubKey proto) {
        try {
            PublicKey publicKey = KeyGeneration.generatePublic(proto.getPublicKey().toByteArray());
            return new PubKey(publicKey, proto.getKeyId());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getHash() {
        if (hash == null) {
            hash = DigestUtil.hash(publicKey.getEncoded());
        }
        return hash;
    }

    public String getId() {
        if (id == null) {
            id = Hex.encode(getHash());
        }
        return id;
    }
}