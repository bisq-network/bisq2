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

import bisq.api.access.identity.DeviceProfile;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionMapping;
import bisq.api.access.permissions.PermissionService;
import bisq.security.SignatureUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PairingService {
    private final static long PAIRING_CODE_TTL = TimeUnit.MINUTES.toMillis(5);

    private final PermissionService<? extends PermissionMapping> permissionService;

    private final Map<String, PairingCode> pairingCodeByIdMap = new ConcurrentHashMap<>();
    private final Map<UUID, DeviceProfile> deviceProfileByIdMap = new ConcurrentHashMap<>();

    public PairingService(PermissionService<? extends PermissionMapping> permissionService) {
        this.permissionService = permissionService;
    }

    public PairingCode createPairingCode(Permission requiredPermissions) {
        return createPairingCode(Set.of(requiredPermissions));
    }

    public PairingCode createPairingCode(Set<Permission> grantedPermissions) {
        Instant expiresAt = Instant.now().plusMillis(PAIRING_CODE_TTL);
        String id = UUID.randomUUID().toString();
        PairingCode pairingCode = new PairingCode(id, expiresAt, grantedPermissions);
        pairingCodeByIdMap.put(id, pairingCode);
        return pairingCode;
    }

    public DeviceProfile pairDevice(PairingRequest request) {
        PairingRequestPayload payload = request.getPairingRequestPayload();
        PairingCode pairingCode = pairingCodeByIdMap.get(payload.getPairingCodeId());
        if (pairingCode == null) {
            throw new SecurityException("Pairing code not found or already used");
        }

        if (isExpired(pairingCode)) {
            throw new SecurityException("Pairing code is expired");
        }
        if (isSignatureInvalid(request)) {
            throw new SecurityException("Invalid signature");
        }

        markUsed(pairingCode);

        UUID deviceId = UUID.randomUUID();
        DeviceProfile deviceProfile = new DeviceProfile(deviceId, payload.getDeviceName(), payload.getDevicePublicKey());
        deviceProfileByIdMap.put(deviceId, deviceProfile);

        permissionService.setDevicePermissions(deviceId, pairingCode.getGrantedPermissions());

        return deviceProfile;
    }

    public Optional<PairingCode> findPairingCode(String id) {
        return Optional.ofNullable(pairingCodeByIdMap.get(id));
    }

    public Optional<DeviceProfile> findDeviceProfile(UUID id) {
        return Optional.ofNullable(deviceProfileByIdMap.get(id));
    }

    private boolean isExpired(PairingCode pairingCode) {
        return Instant.now().isAfter(pairingCode.getExpiresAt());
    }

    private void markUsed(PairingCode pairingCode) {
        pairingCodeByIdMap.remove(pairingCode.getId());
    }

    private boolean isSignatureInvalid(PairingRequest request) {
        PairingRequestPayload payload = request.getPairingRequestPayload();
        byte[] message = PairingRequestPayloadEncoder.encode(payload);
        byte[] signature = request.getSignature();
        PublicKey publicKey = payload.getDevicePublicKey();
        try {
            SignatureUtil.verify(message, signature, publicKey);
            return false;
        } catch (GeneralSecurityException e) {
            return true;
        }
    }
}
