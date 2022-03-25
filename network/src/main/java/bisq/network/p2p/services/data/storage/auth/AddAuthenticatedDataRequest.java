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

package bisq.network.p2p.services.data.storage.auth;

import bisq.common.encoding.Hex;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.security.DigestUtil;
import bisq.security.KeyGeneration;
import bisq.security.SignatureUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;


@EqualsAndHashCode
@Slf4j
public class AddAuthenticatedDataRequest implements AuthenticatedDataRequest, AddDataRequest {
    public static AddAuthenticatedDataRequest from(AuthenticatedDataStorageService store, AuthenticatedData payload, KeyPair keyPair)
            throws GeneralSecurityException {

        byte[] hash = DigestUtil.hash(payload.serialize());
        byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
        int sequenceNumber = store.getSequenceNumber(hash) + 1;
        AuthenticatedSequentialData data = new AuthenticatedSequentialData(payload, sequenceNumber, pubKeyHash, System.currentTimeMillis());
        byte[] serialized = data.serialize();
        byte[] signature = SignatureUtil.sign(serialized, keyPair.getPrivate());
         /*  log.error("hash={}", Hex.encode(hash));
        log.error("keyPair.getPublic().getEncoded()={}", Hex.encode(keyPair.getPublic().getEncoded()));
        log.error("pubKeyHash={}", Hex.encode(pubKeyHash));
        log.error("sequenceNumber={}", sequenceNumber);
        log.error("serialized={}", Hex.encode(serialized));
        log.error("signature={}", Hex.encode(signature));
        log.error("data={}", data);*/
        return new AddAuthenticatedDataRequest(data, signature, keyPair.getPublic());
    }

    @Getter
    protected final AuthenticatedSequentialData authenticatedSequentialData;
    @Getter
    protected final byte[] signature;         // 256 bytes
    @Getter
    protected final byte[] ownerPublicKeyBytes; // 294 bytes
    @Nullable
    transient protected PublicKey ownerPublicKey;

    public AddAuthenticatedDataRequest(AuthenticatedSequentialData authenticatedSequentialData, byte[] signature, PublicKey ownerPublicKey) {
        this(authenticatedSequentialData,
                signature,
                ownerPublicKey.getEncoded(),
                ownerPublicKey);
    }

    protected AddAuthenticatedDataRequest(AuthenticatedSequentialData authenticatedSequentialData,
                                          byte[] signature,
                                          byte[] ownerPublicKeyBytes,
                                          PublicKey ownerPublicKey) {
        this.authenticatedSequentialData = authenticatedSequentialData;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.signature = signature;
    }

    public boolean isSignatureInvalid() {
        try {
         /*   log.error("authenticatedData={}", authenticatedData);
            log.error("authenticatedData.serialize()={}", Hex.encode(authenticatedData.serialize()));
            log.error("signature={}", Hex.encode(signature));
            log.error("getOwnerPublicKey()={}", Hex.encode(getOwnerPublicKey().getEncoded()));*/

            return !SignatureUtil.verify(authenticatedSequentialData.serialize(), signature, getOwnerPublicKey());
        } catch (Exception e) {
            log.warn(e.toString(), e);
            return true;
        }
    }

    public boolean isPublicKeyInvalid() {
        try {
            return !Arrays.equals(authenticatedSequentialData.getPubKeyHash(), DigestUtil.hash(ownerPublicKeyBytes));
        } catch (Exception e) {
            return true;
        }
    }

    public PublicKey getOwnerPublicKey() {
        return Optional.ofNullable(ownerPublicKey).orElseGet(() -> {
            try {
                return KeyGeneration.generatePublic(ownerPublicKeyBytes);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public String getFileName() {
        return authenticatedSequentialData.getAuthenticatedData().getMetaData().getFileName();
    }

    @Override
    public int getSequenceNumber() {
        return authenticatedSequentialData.getSequenceNumber();
    }

    @Override
    public long getCreated() {
        return authenticatedSequentialData.getCreated();
    }

    public MetaData getMetaData() {
        return authenticatedSequentialData.getAuthenticatedData().getMetaData();
    }

    @Override
    public String toString() {
        return "AddAuthenticatedDataRequest{" +
                "\r\n     authenticatedData=" + authenticatedSequentialData +
                ",\r\n     signature=" + Hex.encode(signature) +
                ",\r\n     ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                "\r\n}";
    }
}
