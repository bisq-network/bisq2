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

package network.misq.network.p2p.services.data.storage.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.encoding.Hex;
import network.misq.network.p2p.services.data.storage.MetaData;
import network.misq.security.DigestUtil;
import network.misq.security.SignatureUtil;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;

@Getter
@EqualsAndHashCode
@Slf4j
public class AddAuthenticatedDataRequest implements AuthenticatedDataRequest, Serializable {

    public static AddAuthenticatedDataRequest from(AuthenticatedDataStore store, AuthenticatedPayload payload, KeyPair keyPair)
            throws GeneralSecurityException {
        byte[] hash = DigestUtil.hash(payload.serialize());
        byte[] hashOfPublicKey = DigestUtil.hash(keyPair.getPublic().getEncoded());
        int newSequenceNumber = store.getSequenceNumber(hash) + 1;
        AuthenticatedData data = new AuthenticatedData(payload, newSequenceNumber, hashOfPublicKey, System.currentTimeMillis());
        byte[] serialized = data.serialize();
        byte[] signature = SignatureUtil.sign(serialized, keyPair.getPrivate());
        return new AddAuthenticatedDataRequest(data, signature, keyPair.getPublic());
    }

    protected final AuthenticatedData authenticatedData;
    protected final byte[] signature;         // 256 bytes
    protected final byte[] ownerPublicKeyBytes; // 294 bytes
    transient protected final PublicKey ownerPublicKey;

    public AddAuthenticatedDataRequest(AuthenticatedData authenticatedData, byte[] signature, PublicKey ownerPublicKey) {
        this(authenticatedData,
                signature,
                ownerPublicKey.getEncoded(),
                ownerPublicKey);
    }

    protected AddAuthenticatedDataRequest(AuthenticatedData authenticatedData,
                                          byte[] signature,
                                          byte[] ownerPublicKeyBytes,
                                          PublicKey ownerPublicKey) {
        this.authenticatedData = authenticatedData;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.signature = signature;
    }

    public boolean isSignatureInvalid() {
        try {
            return !SignatureUtil.verify(authenticatedData.serialize(), signature, ownerPublicKey);
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isPublicKeyInvalid() {
        try {
            return !Arrays.equals(authenticatedData.getHashOfPublicKey(), DigestUtil.hash(ownerPublicKeyBytes));
        } catch (Exception e) {
            return true;
        }
    }

    public String getFileName() {
        return authenticatedData.getPayload().getMetaData().getFileName();
    }

    @Override
    public int getSequenceNumber() {
        return authenticatedData.getSequenceNumber();
    }

    @Override
    public long getCreated() {
        return authenticatedData.getCreated();
    }

    public MetaData getMetaData() {
        return authenticatedData.getPayload().getMetaData();
    }

    @Override
    public String toString() {
        return "AddProtectedDataRequest{" +
                "\r\n     entry=" + authenticatedData +
                ",\r\n     signature=" + Hex.encode(signature) +
                ",\r\n     ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                "\r\n}";
    }

}
