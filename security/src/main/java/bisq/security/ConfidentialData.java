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
import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@EqualsAndHashCode
public final class ConfidentialData implements NetworkProto {
    private static final int MAX_SIZE_CIPHERTEXT = 20_000;

    private final byte[] senderPublicKey;
    private final byte[] iv;
    private final byte[] cipherText;    // message with 1000 chars has about 1500 bytes
    private final byte[] signature;

    public ConfidentialData(byte[] senderPublicKey,
                            byte[] iv,
                            byte[] cipherText,
                            byte[] signature) {
        this.senderPublicKey = senderPublicKey;
        this.iv = iv;
        this.cipherText = cipherText;
        this.signature = signature;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(iv.length <= 20);
        checkArgument(cipherText.length <= MAX_SIZE_CIPHERTEXT);
        NetworkDataValidation.validateECPubKey(senderPublicKey);
        NetworkDataValidation.validateECSignature(signature);
    }

    @Override
    public bisq.security.protobuf.ConfidentialData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.security.protobuf.ConfidentialData.Builder getBuilder(boolean serializeForHash) {
        return bisq.security.protobuf.ConfidentialData.newBuilder()
                .setSenderPublicKey(ByteString.copyFrom(senderPublicKey))
                .setIv(ByteString.copyFrom(iv))
                .setCipherText(ByteString.copyFrom(cipherText))
                .setSignature(ByteString.copyFrom(signature));
    }

    public static ConfidentialData fromProto(bisq.security.protobuf.ConfidentialData proto) {
        return new ConfidentialData(proto.getSenderPublicKey().toByteArray(),
                proto.getIv().toByteArray(),
                proto.getCipherText().toByteArray(),
                proto.getSignature().toByteArray());
    }

    public double getCostFactor() {
        return cipherText.length / (double) MAX_SIZE_CIPHERTEXT;
    }

    @Override
    public String toString() {
        return "ConfidentialData{" +
                "senderPublicKey=" + Hex.encode(senderPublicKey) +
                ", iv=" + Hex.encode(iv) +
                ", cipherText=" + Hex.encode(cipherText) +
                ", signature=" + Hex.encode(signature) +
                '}';
    }
}
