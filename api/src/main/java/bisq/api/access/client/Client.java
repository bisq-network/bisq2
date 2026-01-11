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

package bisq.api.access.client;

import bisq.api.access.pairing.PairingRequest;
import bisq.api.access.pairing.PairingRequestPayload;
import bisq.api.access.pairing.PairingRequestPayloadEncoder;
import bisq.api.access.permissions.Permission;
import bisq.api.access.session.SessionToken;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import lombok.Getter;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Just for dev testing and mocks. Represents the mobile client
 */
@Getter
public abstract class Client {
    protected String qrCode;
    protected SessionToken sessionToken;
    protected Set<Permission> grantedPermissions;

    public ClientIdentity createClientIdentity(String deviceName) {
        KeyPair keyPair = KeyGeneration.generateKeyPair();
        // TODO on Mobile client: Allow user to set device name, store ClientIdentity
        return new ClientIdentity(deviceName, keyPair);
    }

    // TODO on Mobile client: implement reader
    public abstract CompletableFuture<String> readQrCode();

    public PairingRequest createPairingRequest(String pairingCodeId,
                                               ClientIdentity clientIdentity) throws GeneralSecurityException {
        PairingRequestPayload payload = new PairingRequestPayload(pairingCodeId,
                clientIdentity.getPublicKey(),
                clientIdentity.getDeviceName(),
                Instant.now());
        byte[] signature = sign(payload, clientIdentity.getKeyPair().getPrivate());
        return new PairingRequest(payload, signature);
    }

    private byte[] sign(PairingRequestPayload payload, PrivateKey privateKey) throws GeneralSecurityException {
        byte[] message = PairingRequestPayloadEncoder.encode(payload);
        return SignatureUtil.sign(message, privateKey);
    }

    // TODO on Mobile client: implement sending of request
    public abstract CompletableFuture<SessionToken> sendRequest(PairingRequest request);
}
