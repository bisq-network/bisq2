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

package bisq.security;

import bisq.common.encoding.Hex;
import bisq.common.proto.PersistableProto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * <p>An instance of EncryptedData is a holder for an initialization vector and encrypted bytes.</p>
 *
 * <p>The initialisation vector is random data that is used to initialise the AES block cipher when the
 * private key bytes were encrypted. You need these for decryption.</p>
 */
@Getter
@EqualsAndHashCode
public final class EncryptedData implements PersistableProto {
    private final byte[] iv;
    private final byte[] cipherText;

    public EncryptedData(byte[] iv, byte[] cipherText) {
        this.iv = iv;
        this.cipherText = cipherText;
    }

    @Override
    public bisq.security.protobuf.EncryptedData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.security.protobuf.EncryptedData.Builder getBuilder(boolean serializeForHash) {
        return bisq.security.protobuf.EncryptedData.newBuilder()
                .setIv(ByteString.copyFrom(iv))
                .setCipherText(ByteString.copyFrom(cipherText));
    }

    public static EncryptedData fromProto(bisq.security.protobuf.EncryptedData proto) {
        return new EncryptedData(proto.getIv().toByteArray(), proto.getCipherText().toByteArray());
    }

    @Override
    public String toString() {
        return "EncryptedData{" +
                "\r\n     iv=" + Hex.encode(iv) +
                ",\r\n     cipherText=" + Hex.encode(cipherText) +
                "\r\n}";
    }
}