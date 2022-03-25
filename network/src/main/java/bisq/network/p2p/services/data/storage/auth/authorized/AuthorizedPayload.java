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

package bisq.network.p2p.services.data.storage.auth.authorized;

import bisq.common.encoding.Hex;
import bisq.network.p2p.services.data.storage.NetworkPayload;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.security.SignatureUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Set;

/**
 * Data which is signed by an authorized key (e.g. Filter, Alert, DisputeAgent...)
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public abstract class AuthorizedPayload extends AuthenticatedPayload {
    private final NetworkPayload networkPayload;
    private final byte[] signature;
    private final byte[] authorizedPublicKeyBytes;
    transient private final PublicKey authorizedPublicKey;

    public AuthorizedPayload(NetworkPayload networkPayload, byte[] signature, PublicKey authorizedPublicKey) {
        super(null, null); //todo
        this.networkPayload = networkPayload;
        this.signature = signature;
        this.authorizedPublicKey = authorizedPublicKey;
        authorizedPublicKeyBytes = authorizedPublicKey.getEncoded();
    }

    @Override
    public boolean isDataInvalid() {
        try {
            return networkPayload.isDataInvalid() ||
                    !getAuthorizedPublicKeys().contains(Hex.encode(authorizedPublicKeyBytes)) ||
                    !SignatureUtil.verify(networkPayload.serialize(), signature, authorizedPublicKey);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return true;
        }
    }

    public abstract Set<String> getAuthorizedPublicKeys();
}
