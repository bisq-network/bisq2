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

package bisq.contract;

import bisq.common.encoding.Hex;
import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

@Slf4j
@Getter
@EqualsAndHashCode
public class ContractSignatureData implements NetworkProto {
    private final byte[] contractHash;
    private final byte[] signature;
    private final PublicKey publicKey;

    public ContractSignatureData(byte[] contractHash, byte[] signature, PublicKey publicKey) {
        this.contractHash = contractHash;
        this.signature = signature;
        this.publicKey = publicKey;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(contractHash);
        NetworkDataValidation.validateECSignature(signature);
        NetworkDataValidation.validateECPubKey(publicKey);
    }

    @Override
    public bisq.contract.protobuf.ContractSignatureData.Builder getBuilder(boolean serializeForHash) {
        return bisq.contract.protobuf.ContractSignatureData.newBuilder()
                .setContractHash(ByteString.copyFrom(contractHash))
                .setSignature(ByteString.copyFrom(signature))
                .setPublicKeyBytes(ByteString.copyFrom(publicKey.getEncoded()));
    }

    @Override
    public bisq.contract.protobuf.ContractSignatureData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static ContractSignatureData fromProto(bisq.contract.protobuf.ContractSignatureData proto) {
        try {
            PublicKey publicKey = KeyGeneration.generatePublic(proto.getPublicKeyBytes().toByteArray());
            return new ContractSignatureData(proto.getContractHash().toByteArray(),
                    proto.getSignature().toByteArray(),
                    publicKey);
        } catch (GeneralSecurityException e) {
            log.error("Could not generate key from protobuf ContractSignatureData.publicKeyBytes", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "ContractSignatureData{" +
                "\r\n     contractHash=" + Hex.encode(contractHash) +
                ",\r\n     signature=" + Hex.encode(signature) +
                ",\r\n     publicKey=" + publicKey +
                "\r\n}";
    }
}