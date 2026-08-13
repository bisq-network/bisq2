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

package bisq.api.access.persistence;

import bisq.api.access.identity.ClientProfile;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
final class ApiAccessStore implements PersistableStore<ApiAccessStore> {
    // Informational only — a pre-grantAll node rewrites the store from its own model and drops
    // this field, so it cannot survive a downgrade and MUST NOT gate any security decision.
    // Promotion to grantAll is evidence-based instead; see promoteIfFullStandardGrant.
    private static final int PERMISSIONS_SCHEMA_VERSION = 1;

    private transient boolean promotedEntriesDuringLoad;

    @Getter(AccessLevel.PACKAGE)
    private final Map<String, ClientProfile> clientProfileByIdMap = new ConcurrentHashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Map<String, PermissionSet> permissionsByClientId = new ConcurrentHashMap<>();

    ApiAccessStore() {
        this(new HashMap<>(), new HashMap<>());
    }

    private ApiAccessStore(Map<String, ClientProfile> clientProfileByIdMap,
                           Map<String, PermissionSet> permissionsByClientId) {
        this.clientProfileByIdMap.putAll(clientProfileByIdMap);
        this.permissionsByClientId.putAll(permissionsByClientId);
    }

    @Override
    public bisq.api.protobuf.ApiAccessStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.api.protobuf.ApiAccessStore.newBuilder()
                .putAllClientProfileByIdMap(clientProfileByIdMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toProto(serializeForHash))))
                .putAllPermissionsByClientId(permissionsByClientId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toProto(serializeForHash))))
                .setPermissionsSchemaVersion(PERMISSIONS_SCHEMA_VERSION);
    }

    @Override
    public bisq.api.protobuf.ApiAccessStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static ApiAccessStore fromProto(bisq.api.protobuf.ApiAccessStore proto) {
        Map<String, ClientProfile> clientProfileByIdMap = proto.getClientProfileByIdMapMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> ClientProfile.fromProto(e.getValue())));
        Map<String, PermissionSet> permissionsByClientId = new HashMap<>();
        boolean anyPromoted = false;
        for (Map.Entry<String, bisq.api.protobuf.PermissionSet> e : proto.getPermissionsByClientIdMap().entrySet()) {
            PermissionSet loaded = PermissionSet.fromProto(e.getValue());
            PermissionSet effective = promoteIfFullStandardGrant(e.getKey(), loaded);
            anyPromoted |= effective != loaded;
            permissionsByClientId.put(e.getKey(), effective);
        }
        ApiAccessStore store = new ApiAccessStore(clientProfileByIdMap, permissionsByClientId);
        store.promotedEntriesDuringLoad = anyPromoted;
        return store;
    }

    /**
     * Whether this load promoted any entry to grantAll. The promotion happens in memory only —
     * {@code ApiAccessStoreService#onPersistedApplied} checks this flag and persists, so the
     * promotion reaches disk on the same boot that computed it. Without that write-back, a node
     * that never pairs a new client would keep the old explicit list on disk, and a later
     * version with more permissions would no longer recognise it as a full grant (the client
     * would silently fall back to a restricted set).
     */
    boolean hadPromotedEntriesDuringLoad() {
        return promotedEntriesDuringLoad;
    }

    /**
     * Evidence-based promotion, applied on every load and deliberately NOT gated on
     * {@code permissionsSchemaVersion}: no in-store marker survives a downgrade, because a
     * pre-grantAll node rebuilds the proto from its own domain model on persist and thereby
     * drops fields it does not know. Trusting a version stamp would let a downgrade/upgrade
     * cycle silently promote a deliberately restricted entry to full access.
     * <p>
     * Instead the entry's own content is the evidence: only a set EXACTLY equal to the running
     * version's auto-grantable ("standard") set is promoted — at that moment promotion grants
     * nothing extra, it only keeps the grant covering standard permissions added by future
     * versions (the semantics every full grant was issued with). Strict equality, not
     * containsAll: a set that additionally holds a sensitive permission must stay explicit,
     * otherwise promotion would silently drop the sensitive grant from the grantAll expansion.
     * <p>
     * Trade-off: a full grant persisted by an older version and first re-read by a binary that
     * has since gained permissions is NOT promoted and that client keeps the old set (fails
     * closed; re-pairing restores full access) — mitigated by the persist-after-promotion in
     * {@code ApiAccessStoreService#onPersistedApplied}.
     * <p>
     * KNOWN RESIDUAL (not reachable today — no live path persists a genuinely restricted
     * non-grantAll grant; every pairing grants the full standard set, folded to grantAll at
     * write time by {@code PermissionService#putPermissions}). The equality is against the
     * RUNNING binary's autoGrantable set, so a deliberately restricted grant is indistinguishable
     * from a full grant of an OLDER version whose standard set happened to equal it: read by that
     * older binary it would be promoted (and persisted) as grantAll, then re-expand on upgrade —
     * regaining a standard permission it was never granted. This becomes reachable only once a
     * feature can persist a genuinely restricted grant (granular-permission editor / sensitive-
     * permission per-device flow); that feature must first introduce a downgrade-durable marker
     * distinguishing "deliberate restriction" from "legacy full grant", or accept this as a
     * documented residual. Sensitive permissions are unaffected either way: a set containing one
     * can never equal any version's autoGrantable set, so it is never promotable.
     */
    private static PermissionSet promoteIfFullStandardGrant(String clientId, PermissionSet permissionSet) {
        if (permissionSet.isGrantAll()) {
            return permissionSet;
        }
        if (permissionSet.getPermissions().equals(Permission.autoGrantable())) {
            log.info("Promoting full standard permission set of client {} to grantAll", clientId);
            return PermissionSet.grantAll();
        }
        return permissionSet;
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.api.protobuf.ApiAccessStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public ApiAccessStore getClone() {
        return new ApiAccessStore(Map.copyOf(clientProfileByIdMap),
                Map.copyOf(permissionsByClientId)
        );
    }

    @Override
    public void applyPersisted(ApiAccessStore persisted) {
        clientProfileByIdMap.clear();
        permissionsByClientId.clear();
        clientProfileByIdMap.putAll(persisted.getClientProfileByIdMap());
        permissionsByClientId.putAll(persisted.getPermissionsByClientId());
    }
}