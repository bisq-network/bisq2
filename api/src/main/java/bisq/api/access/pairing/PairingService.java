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
import bisq.api.access.pairing.qr.TextQrCodeRenderer;
import bisq.api.access.permissions.Permission;
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PairingService {
    public static final byte VERSION = 1;

    /**
     * The client name is free text supplied by the pairing client and is rendered in the host UI,
     * so it is capped. Truncating instead of rejecting keeps pairing working for clients that
     * derive the name from a long device model string.
     */
    public static final int MAX_CLIENT_NAME_LENGTH = 100;

    private final ApiConfig apiConfig;
    private final Path appDataDirPath;
    private final ApiAccessStoreService apiAccessStoreService;
    private final PermissionService permissionService;
    @Getter
    private final int pairingCodeTtlInSeconds;
    private final Map<String, PairingCode> pairingCodeByIdMap = new ConcurrentHashMap<>();
    @Getter
    private final Observable<PairingCode> pairingCode = new Observable<>();
    @Getter
    private final Observable<String> pairingQrCode = new Observable<>();

    public PairingService(ApiConfig apiConfig,
                          Path appDataDirPath,
                          ApiAccessStoreService apiAccessStoreService,
                          PermissionService permissionService) {
        this.apiConfig = apiConfig;
        this.appDataDirPath = appDataDirPath;
        this.apiAccessStoreService = apiAccessStoreService;
        this.permissionService = permissionService;

        pairingCodeTtlInSeconds = apiConfig.getPairingCodeTtlInSeconds();
    }

    public PairingCode createPairingCode(Permission requiredPermissions) {
        return createPairingCode(Set.of(requiredPermissions));
    }

    public PairingCode createPairingCode(Set<Permission> grantedPermissions) {
        Instant expiresAt = Instant.now().plusSeconds(pairingCodeTtlInSeconds);
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

        // Validated before the pairing code is consumed so a rejected request does not burn it.
        if (clientName == null || clientName.isBlank()) {
            throw new InvalidPairingRequestException("Client name must not be blank");
        }
        String cappedClientName = capClientName(clientName);

        // Atomic remove to prevent race conditions - ensures only one request can use the code
        PairingCode pairingCode = pairingCodeByIdMap.remove(pairingCodeId);
        if (pairingCode == null) {
            throw new InvalidPairingRequestException("Pairing code not found or already used");
        }

        if (isExpired(pairingCode)) {
            throw new InvalidPairingRequestException("Pairing code is expired");
        }

        // Signal that the active code was consumed so observers can regenerate
        log.info("Pairing code {} consumed, signalling observers for regeneration", pairingCodeId);
        this.pairingCode.set(null);

        String clientId = UUID.randomUUID().toString();
        byte[] secret = ByteArrayUtils.getRandomBytes(32);
        String clientSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        ClientProfile clientProfile = new ClientProfile(clientId,
                clientSecret,
                cappedClientName);
        // Profile and grant in one step: written separately, a revocation could land between them
        // and leave a grant behind that authorizes the client it had just revoked.
        apiAccessStoreService.putClientProfileAndPermissions(clientId,
                clientProfile,
                permissionService.toPermissionSet(pairingCode.getGrantedPermissions()));

        return clientProfile;
    }

    public Optional<PairingCode> findPairingCode(String id) {
        return Optional.ofNullable(pairingCodeByIdMap.get(id));
    }

    public Optional<ClientProfile> findClientProfile(String id) {
        return Optional.ofNullable(apiAccessStoreService.getClientProfileByIdMap().get(id));
    }

    public List<ClientProfile> getClientProfiles() {
        return List.copyOf(apiAccessStoreService.getClientProfileByIdMap().values());
    }

    /**
     * Removes the client profile and associated permissions for the given client ID.
     *
     * @param clientId The client ID to revoke
     * @return {@code true} if the profile was found and removed; {@code false} if not found
     */
    public boolean revokeClientProfile(String clientId) {
        return apiAccessStoreService.removeClientProfile(clientId);
    }

    /**
     * Caps the name at {@link #MAX_CLIENT_NAME_LENGTH} chars without splitting a surrogate pair,
     * so a name ending in an emoji is shortened rather than corrupted into a lone surrogate.
     */
    private static String capClientName(String clientName) {
        if (clientName.length() <= MAX_CLIENT_NAME_LENGTH) {
            return clientName;
        }
        int end = Character.isHighSurrogate(clientName.charAt(MAX_CLIENT_NAME_LENGTH - 1))
                ? MAX_CLIENT_NAME_LENGTH - 1
                : MAX_CLIENT_NAME_LENGTH;
        return clientName.substring(0, end);
    }

    private boolean isExpired(PairingCode pairingCode) {
        return Instant.now().isAfter(pairingCode.getExpiresAt());
    }

    /**
     * Removes all expired pairing codes from the map.
     * Should be called periodically or when creating new pairing codes.
     */
    public void cleanupExpiredPairingCodes() {
        int removed = 0;
        for (Map.Entry<String, PairingCode> entry : pairingCodeByIdMap.entrySet()) {
            if (isExpired(entry.getValue())) {
                pairingCodeByIdMap.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned up {} expired pairing codes", removed);
        }
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

    // Package-private for testing
    void writePairingQrCodeToDataDir(String pairingQrCode) {
        Path path = appDataDirPath.resolve("pairing_qr_code.txt");
        try {
            StringBuilder content = new StringBuilder(pairingQrCode);
            try {
                content.append("\n\n\n").append(TextQrCodeRenderer.render(pairingQrCode));
            } catch (Exception e) {
                log.warn("Failed to render text QR code", e);
            }
            // The file is rewritten on every code regeneration. Write to a temp file and move
            // atomically so a concurrent reader never observes a truncated payload.
            Path tempPath = appDataDirPath.resolve("pairing_qr_code.txt.tmp");
            FileMutatorUtils.writeToPath(content.toString(), tempPath);
            try {
                Files.move(tempPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // ATOMIC_MOVE with an existing target is implementation specific and can fail
                // with a plain IOException (e.g. a concurrent reader holding the file on
                // Windows). Losing atomicity for one rotation is better than not updating.
                log.warn("Atomic move of pairing QR code failed, falling back to non-atomic replace", e);
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Pairing QR code written to {}", path);
        } catch (IOException e) {
            log.error("Error at write pairing QR code to disk at {}", path, e);
        }
    }
}
