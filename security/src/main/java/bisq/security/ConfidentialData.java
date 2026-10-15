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
import bisq.common.encoding.canonical.Canonical;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.validation.NetworkDataValidation;
import bisq.security.keys.EcPublicKeyValidation;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
public final class ConfidentialData implements NetworkProto, Canonical {
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

        // The caller reports invalid data: fromProto for received data, HybridEncryption for data which we create.
        verify();
    }

    @Override
    public void verify() {
        checkArgument(iv.length <= 20);
        checkArgument(cipherText.length <= MAX_SIZE_CIPHERTEXT, "cipherText size must not exceed 20 000 bytes");
        EcPublicKeyValidation.validate(senderPublicKey);
        NetworkDataValidation.validateECSignature(signature);
    }

    @Override
    public bisq.security.protobuf.ConfidentialData toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.security.protobuf.ConfidentialData.Builder getBuilder(boolean serializeForHash) {
        return bisq.security.protobuf.ConfidentialData.newBuilder()
                .setSenderPublicKey(ByteString.copyFrom(senderPublicKey))
                .setIv(ByteString.copyFrom(iv))
                .setCipherText(ByteString.copyFrom(cipherText))
                .setSignature(ByteString.copyFrom(signature));
    }

    public static ConfidentialData fromProto(bisq.security.protobuf.ConfidentialData proto)
            throws UnresolvableProtobufException {
        try {
            return new ConfidentialData(proto.getSenderPublicKey().toByteArray(),
                    proto.getIv().toByteArray(),
                    proto.getCipherText().toByteArray(),
                    proto.getSignature().toByteArray());
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    public static final CanonicalSchema<ConfidentialData> DEFAULT_SCHEMA =
            CanonicalSchema.<ConfidentialData>newBuilder()
                    .bytes(1, ConfidentialData::getSenderPublicKey)
                    .bytes(2, ConfidentialData::getIv)
                    .bytes(3, ConfidentialData::getCipherText)
                    .bytes(4, ConfidentialData::getSignature)
                    .build();

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<ConfidentialData> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY -> DEFAULT_SCHEMA;
            case PROOF_OF_WORK -> DEFAULT_SCHEMA;
            case TRADE_CONTRACT -> DEFAULT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfidentialData that)) return false;

        return Arrays.equals(senderPublicKey, that.senderPublicKey) &&
                Arrays.equals(iv, that.iv) &&
                Arrays.equals(cipherText, that.cipherText) &&
                Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(senderPublicKey);
        result = 31 * result + Arrays.hashCode(iv);
        result = 31 * result + Arrays.hashCode(cipherText);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
}
