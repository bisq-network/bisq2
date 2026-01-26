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

import bisq.api.ApiConfig;
import bisq.api.access.identity.ClientProfile;
import bisq.api.access.pairing.qr.PairingQrCodeGenerator;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionMapping;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.persistence.ApiAccessStoreService;
import bisq.api.access.transport.TlsContext;
import bisq.api.access.transport.TorContext;
import bisq.common.file.FileMutatorUtils;
import bisq.common.observable.Observable;
import bisq.common.util.ByteArrayUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PairingService {
    public static final byte VERSION = 1;
    public static final long PAIRING_CODE_TTL = TimeUnit.MINUTES.toMillis(5);

    private final ApiConfig apiConfig;
    private final Path appDataDirPath;
    private final ApiAccessStoreService apiAccessStoreService;
    private final PermissionService<? extends PermissionMapping> permissionService;
    private final Map<String, PairingCode> pairingCodeByIdMap = new ConcurrentHashMap<>();
    @Getter
    private final Observable<PairingCode> pairingCode = new Observable<>();
    @Getter
    private final Observable<String> pairingQrCode = new Observable<>();

    public PairingService(ApiConfig apiConfig,
                          Path appDataDirPath,
                          ApiAccessStoreService apiAccessStoreService,
                          PermissionService<? extends PermissionMapping> permissionService) {
        this.apiConfig = apiConfig;
        this.appDataDirPath = appDataDirPath;
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
        pairingCodeByIdMap.put(id, pairingCode);
        this.pairingCode.set(pairingCode);
        return pairingCode;
    }

    public ClientProfile requestPairing(byte version,
                                        String pairingCodeId,
                                        String clientName) throws InvalidPairingRequestException {
        if (version != VERSION) {
            throw new InvalidPairingRequestException("Unsupported pairing protocol version: " + version);
        }
        PairingCode pairingCode = pairingCodeByIdMap.get(pairingCodeId);
        if (pairingCode == null) {
            throw new InvalidPairingRequestException("Pairing code not found or already used");
        }

        if (isExpired(pairingCode)) {
            pairingCodeByIdMap.remove(pairingCodeId, pairingCode);
            throw new InvalidPairingRequestException("Pairing code is expired");
        }

        // Mark used by removing it
        pairingCodeByIdMap.remove(pairingCodeId, pairingCode);

        String clientId = UUID.randomUUID().toString();
        byte[] secret = ByteArrayUtils.getRandomBytes(32);
        String clientSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        ClientProfile clientProfile = new ClientProfile(clientId,
                clientSecret,
                clientName);
        apiAccessStoreService.putClientProfile(clientId, clientProfile);

        permissionService.putPermissions(clientId, pairingCode.getGrantedPermissions());

        return clientProfile;
    }

    public Optional<PairingCode> findPairingCode(String id) {
        return Optional.ofNullable(pairingCodeByIdMap.get(id));
    }

    public Optional<ClientProfile> findClientProfile(String id) {
        return Optional.ofNullable(apiAccessStoreService.getClientProfileByIdMap().get(id));
    }

    private boolean isExpired(PairingCode pairingCode) {
        return Instant.now().isAfter(pairingCode.getExpiresAt());
    }

    public void createPairingQrCode(PairingCode pairingCode,
                                    String webSocketUrl,
                                    Optional<TlsContext> tlsContext,
                                    Optional<TorContext> torContext) {
        try {
            String qrCode = PairingQrCodeGenerator.generateQrCode(pairingCode,
                    webSocketUrl,
                    tlsContext,
                    torContext);
            if (apiConfig.isWritePairingQrCodeToDisk()) {
                writePairingQrCodeToDataDir(qrCode);
            }
            pairingQrCode.set(qrCode);
        } catch (Exception e) {
            log.warn("Could not create QR code", e);
            pairingQrCode.set(null);
        }
    }

    private void writePairingQrCodeToDataDir(String pairingQrCode) {
        try {
            Path path = appDataDirPath.resolve("pairing_qr_code.txt");
            FileMutatorUtils.writeToPath(pairingQrCode, path);
        } catch (IOException e) {
            log.error("Error at write pairing QR code to disk", e);
        }
    }
}
