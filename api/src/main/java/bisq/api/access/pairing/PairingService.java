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

package bisq.api.access.pairing;

import bisq.api.access.ApiAccessStoreService;
import bisq.api.access.identity.ClientProfile;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionMapping;
import bisq.api.access.permissions.PermissionService;
import bisq.common.util.ByteArrayUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PairingService {
    public static final byte VERSION = 1;
    private static final long PAIRING_CODE_TTL = TimeUnit.MINUTES.toMillis(5);

    private final ApiAccessStoreService apiAccessStoreService;
    private final PermissionService<? extends PermissionMapping> permissionService;

    public PairingService(ApiAccessStoreService apiAccessStoreService,
                          PermissionService<? extends PermissionMapping> permissionService) {
        this.apiAccessStoreService = apiAccessStoreService;
        this.permissionService = permissionService;
    }

    public PairingCode createPairingCode(Permission requiredPermissions) {
        return createPairingCode(Set.of(requiredPermissions));
    }

    public PairingCode createPairingCode(Set<Permission> grantedPermissions) {
        Instant expiresAt = Instant.now().plusMillis(PAIRING_CODE_TTL);
        String id = UUID.randomUUID().toString();
        PairingCode pairingCode = new PairingCode(id, expiresAt, Set.copyOf(grantedPermissions));
        apiAccessStoreService.putPairingCode(id, pairingCode);
        return pairingCode;
    }

    public ClientProfile requestPairing(byte version,
                                        String pairingCodeId,
                                        String clientName) throws InvalidPairingRequestException {
        if (version != VERSION) {
            throw new InvalidPairingRequestException("Unsupported pairing protocol version: " + version);
        }
        PairingCode pairingCode = apiAccessStoreService.getPairingCodeByIdMap().get(pairingCodeId);
        if (pairingCode == null) {
            throw new InvalidPairingRequestException("Pairing code not found or already used");
        }

        if (isExpired(pairingCode)) {
            apiAccessStoreService.removePairingCode(pairingCodeId, pairingCode);
            throw new InvalidPairingRequestException("Pairing code is expired");
        }

        // Mark used by removing it
        apiAccessStoreService.removePairingCode(pairingCodeId, pairingCode);

        String clientId = UUID.randomUUID().toString();
        byte[] secret = ByteArrayUtils.getRandomBytes(32);
        String clientSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        ClientProfile clientProfile = new ClientProfile(clientId,
                clientSecret,
                clientName);
        apiAccessStoreService.putClientProfile(clientId, clientProfile);

        permissionService.setPermissions(clientId, pairingCode.getGrantedPermissions());

        return clientProfile;
    }

    public Optional<PairingCode> findPairingCode(String id) {
        return Optional.ofNullable(apiAccessStoreService.getPairingCodeByIdMap().get(id));
    }

    public Optional<ClientProfile> findClientProfile(String id) {
        return Optional.ofNullable(apiAccessStoreService.getClientProfileByIdMap().get(id));
    }

    private boolean isExpired(PairingCode pairingCode) {
        return Instant.now().isAfter(pairingCode.getExpiresAt());
    }
}
