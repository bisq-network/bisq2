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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
final class ApiAccessStore implements PersistableStore<ApiAccessStore> {
    // Informational only — a pre-grantAll node rewrites the store from its own model and drops
    // this field, so it cannot survive a downgrade and MUST NOT gate any security decision.
    // Promotion to grantAll is evidence-based instead; see promoteIfFullStandardGrant.
    private static final int PERMISSIONS_SCHEMA_VERSION = 1;

    /**
     * The full standard sets of released versions, so a store written by one of them is still
     * recognised as the full grant it was issued as.
     * <p>
     * Kept in code and not in the store on purpose: nothing persisted survives a downgrade, which
     * is the whole reason the promotion below reads the entry's content as evidence. Spelled out
     * value by value rather than derived from a range or an id bound, because the deliberate hole
     * at enum id 11 (proto 12) is exactly the shape that makes a derived set quietly stop matching
     * what shipped.
     * <p>
     * One entry per shape that shipped, and these are expected to be enough for good: the first
     * release carrying grantAll promotes every store it reads and persists the result, and a store
     * that never meets that release still holds one of these same sets however many versions later
     * it is opened. Every pairing on those releases granted the full set
     * ({@code ApiConfig#grantedPermissions} was {@code Set.of(Permission.values())}), all standard,
     * and a grant is only rewritten at pairing time, so a client that paired on v2.1.9 and never
     * re-paired still holds the v2.1.9 shape whatever it upgraded to since.
     * <p>
     * Every permission listed here must stay standard: promotion replaces the explicit list with
     * the grantAll expansion, which never covers sensitive permissions, so reclassifying a listed
     * permission as sensitive requires pruning it from these sets first.
     */
    static final Set<Set<Permission>> RELEASED_FULL_STANDARD_SETS = Set.of(
            // v2.1.9, the first release with this store: 10 permissions, up to USER_PROFILES.
            Set.of(Permission.TRADE_CHAT_CHANNELS,
                    Permission.EXPLORER,
                    Permission.MARKET_PRICE,
                    Permission.OFFERBOOK,
                    Permission.PAYMENT_ACCOUNTS,
                    Permission.REPUTATION,
                    Permission.SETTINGS,
                    Permission.TRADES,
                    Permission.USER_IDENTITIES,
                    Permission.USER_PROFILES),
            // v2.1.10 to v2.1.12: the same plus MOBILE_DEVICES.
            Set.of(Permission.TRADE_CHAT_CHANNELS,
                    Permission.EXPLORER,
                    Permission.MARKET_PRICE,
                    Permission.OFFERBOOK,
                    Permission.PAYMENT_ACCOUNTS,
                    Permission.REPUTATION,
                    Permission.SETTINGS,
                    Permission.TRADES,
                    Permission.USER_IDENTITIES,
                    Permission.USER_PROFILES,
                    Permission.MOBILE_DEVICES));

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
     * {@code permissionsSchemaVersion} in either direction. Not to promote: no in-store marker
     * survives a downgrade, because a pre-grantAll node rebuilds the proto from its own domain
     * model on persist and thereby drops fields it does not know, so trusting a version stamp
     * would let a downgrade/upgrade cycle silently promote a deliberately restricted entry to
     * full access. Not to refuse either: the stamp only says "written by a binary that had the
     * field", and a binary can have the field without knowing the released shapes below — the
     * builds between grantAll and this rule stamp every persist while leaving a v2.1.9-shaped
     * entry explicit — so refusing on the stamp would freeze exactly the grants this rule exists
     * to rescue.
     * <p>
     * Instead the entry's own content is the evidence: only a set EXACTLY equal to the running
     * version's auto-grantable ("standard") set is promoted — at that moment promotion grants
     * nothing extra, it only keeps the grant covering standard permissions added by future
     * versions (the semantics every full grant was issued with). Strict equality, not
     * containsAll: a set that additionally holds a sensitive permission must stay explicit,
     * otherwise promotion would silently drop the sensitive grant from the grantAll expansion.
     * <p>
     * A full grant persisted by an older version counts as the same evidence, through
     * {@link #RELEASED_FULL_STANDARD_SETS}. The two branches differ in how long they match. The
     * running-set branch is a one-shot as long as the standard set only grows: once the binary has
     * grown past a set, that set never equals the running standard set again. Ids are append-only,
     * but that is not what carries it — {@code Permission#autoGrantable} filters on {@code Kind},
     * so reclassifying a standard permission as sensitive would shrink the set, which is what
     * {@code PermissionTest#everyCurrentPermissionIsStandard} guards. The released-set branch is
     * permanent: the released shapes stay in that constant and keep being promoted by every future
     * version, because a store that skipped the first grantAll release still holds exactly one of
     * them however much later it is opened. Leaning on the persist-after-promotion in
     * {@code ApiAccessStoreService#onPersistedApplied} instead would be circular — it writes only
     * when something was promoted, so it covers the second new permission onward and does nothing
     * for the first, which is the one arriving in the same release as grantAll itself.
     * <p>
     * KNOWN RESIDUAL (not reachable today — no live path persists a genuinely restricted
     * non-grantAll grant; every pairing grants the full standard set, folded to grantAll at
     * write time by {@code PermissionService#putPermissions}). A deliberately restricted grant is
     * indistinguishable from a full grant that happens to equal it, on either branch. Running-set
     * branch: read by an OLDER binary whose standard set equals the restriction, it is promoted
     * (and persisted) as grantAll, then re-expands on upgrade — regaining a standard permission it
     * was never granted. Released-set branch, and this one does not fade with versions: a grant of
     * exactly one of the released shapes (the v2.1.9 or the v2.1.12 set — a natural "everything
     * except the newer permissions" choice) is widened to grantAll on the next load by every
     * binary from here on, and persisted that way.
     * This becomes reachable only once a feature can persist a genuinely restricted grant
     * (granular-permission editor / sensitive-permission per-device flow); that feature must first
     * introduce a downgrade-durable marker distinguishing "deliberate restriction" from "legacy
     * full grant", and until it does, no code path may issue a released full set as a deliberate
     * restriction. Sensitive permissions are unaffected on the running-set branch by construction:
     * a set containing one can never equal any version's autoGrantable set. On the released-set
     * branch that holds only by convention — the constant is a value list, so a permission listed
     * there that is later reclassified as sensitive would still match and be dropped from the
     * grantAll expansion; {@code ApiAccessStoreTest} pins that every listed permission is standard.
     */
    private static PermissionSet promoteIfFullStandardGrant(String clientId, PermissionSet permissionSet) {
        if (permissionSet.isGrantAll()) {
            return permissionSet;
        }
        Set<Permission> permissions = permissionSet.getPermissions();
        if (permissions.equals(Permission.autoGrantable()) || RELEASED_FULL_STANDARD_SETS.contains(permissions)) {
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