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

package bisq.api.access;

import bisq.api.access.client.Client;
import bisq.api.access.pairing.PairingRequest;
import bisq.api.access.permissions.Permission;
import bisq.api.access.session.SessionToken;
import lombok.Getter;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Getter
public class MockClient extends Client {
    public CompletableFuture<String> readQrCode() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            return qrCode;
        });
    }

    @Override
    public CompletableFuture<SessionToken> sendRequest(PairingRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            while (sessionToken == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
            }
            return sessionToken;
        });
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public void setSessionToken(SessionToken sessionToken) {
        this.sessionToken = sessionToken;
    }

    public void setGrantedPermissions(Set<Permission> grantedPermissions) {
        this.grantedPermissions = grantedPermissions;
    }
}
