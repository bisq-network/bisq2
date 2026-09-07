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

import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionSet;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAccessStoreTest {
    /** What v2.1.12 wrote for every pairing: all the permissions that binary had. */
    private static final Set<Permission> AS_SHIPPED_IN_V2_1_12 = Set.of(Permission.TRADE_CHAT_CHANNELS,
            Permission.EXPLORER,
            Permission.MARKET_PRICE,
            Permission.OFFERBOOK,
            Permission.PAYMENT_ACCOUNTS,
            Permission.REPUTATION,
            Permission.SETTINGS,
            Permission.TRADES,
            Permission.USER_IDENTITIES,
            Permission.USER_PROFILES,
            Permission.MOBILE_DEVICES);

    @Test
    void fullPermissionSetIsPromotedToGrantAllOnLoad() {
        // An entry already covering every permission of the running version is promoted to
        // grantAll: promotion grants nothing extra at that moment, it only keeps the grant
        // covering permissions added later — which is what every full grant was issued to mean.
        // This is how pre-grantAll stores (always full grants) gain forward coverage.
        bisq.api.protobuf.PermissionSet fullEntry = bisq.api.protobuf.PermissionSet.newBuilder()
                .addAllPermissions(EnumSet.allOf(Permission.class).stream().map(Permission::toProtoEnum).toList())
                .build();
        bisq.api.protobuf.ApiAccessStore proto = bisq.api.protobuf.ApiAccessStore.newBuilder()
                .putPermissionsByClientId("legacy-client", fullEntry)
                .build();

        ApiAccessStore store = ApiAccessStore.fromProto(proto);

        PermissionSet migrated = store.getPermissionsByClientId().get("legacy-client");
        assertTrue(migrated.isGrantAll());
        assertEquals(Permission.autoGrantable(), migrated.getPermissions());
    }

    @Test
    void releasedFullGrantIsPromotedAfterTheRunningVersionGainedPermissions() {
        // The case the test above cannot reach: it builds its entry from the RUNNING enum, so it
        // keeps matching whatever gets added and stays green straight through the regression this
        // one pins. Here the list is hardcoded to what v2.1.12 actually wrote, which is the shape
        // every paired client in the field has on disk, and the running binary has gained a
        // permission since.
        //
        // It has to be promoted anyway: the running-set branch of the promotion is a one-shot (ids
        // are append-only, so a set the binary has grown past never equals the running standard set
        // again), which is why the released set is spelled out in its own constant. Miss it and
        // that client keeps a frozen explicit grant for good, losing every standard permission added
        // from here on.
        // Keeps the test from going vacuous: it only asserts something while the running standard
        // set is strictly larger than what that release wrote.
        assertNotEquals(Permission.autoGrantable(), AS_SHIPPED_IN_V2_1_12);

        bisq.api.protobuf.PermissionSet legacyFullGrant = bisq.api.protobuf.PermissionSet.newBuilder()
                .addAllPermissions(AS_SHIPPED_IN_V2_1_12.stream().map(Permission::toProtoEnum).toList())
                .build();
        bisq.api.protobuf.ApiAccessStore proto = bisq.api.protobuf.ApiAccessStore.newBuilder()
                .putPermissionsByClientId("legacy-client", legacyFullGrant)
                .build();

        ApiAccessStore store = ApiAccessStore.fromProto(proto);

        PermissionSet migrated = store.getPermissionsByClientId().get("legacy-client");
        assertTrue(migrated.isGrantAll());
        assertEquals(Permission.autoGrantable(), migrated.getPermissions());
        // The promotion is in memory; this flag is what makes ApiAccessStoreService write it back
        // on the same boot, so the store on disk stops depending on this rule next time.
        assertTrue(store.hadPromotedEntriesDuringLoad());
    }

    @Test
    void restrictedGrantEqualToAReleasedFullSetIsStillPromoted() {
        // The released-set branch does not fade with versions, and this pins the cost of that as
        // current behaviour: a grant of exactly the v2.1.12 set is promoted whatever wrote it,
        // including this binary stamping the current schema version, which is what a deliberate
        // "everything except the newer permissions" restriction would look like once something can
        // issue one. Nothing can today. This test does not catch that feature arriving; it stays
        // green while the promotion happens. It is here so that the feature has to change this test
        // on purpose, and the KNOWN RESIDUAL on ApiAccessStore#promoteIfFullStandardGrant says what
        // it has to add first: a downgrade-durable marker. The stamp is not that marker: builds
        // between grantAll and the released-set rule stamp every persist while leaving a v2.1.9
        // entry explicit, so refusing on it would freeze that client (see the test below).
        assertNotEquals(Permission.autoGrantable(), AS_SHIPPED_IN_V2_1_12);
        bisq.api.protobuf.PermissionSet restriction = bisq.api.protobuf.PermissionSet.newBuilder()
                .addAllPermissions(AS_SHIPPED_IN_V2_1_12.stream().map(Permission::toProtoEnum).toList())
                .build();
        for (int schemaVersion : new int[]{0, 1}) {
            bisq.api.protobuf.ApiAccessStore proto = bisq.api.protobuf.ApiAccessStore.newBuilder()
                    .putPermissionsByClientId("restricted-client", restriction)
                    .setPermissionsSchemaVersion(schemaVersion)
                    .build();

            ApiAccessStore store = ApiAccessStore.fromProto(proto);

            PermissionSet loaded = store.getPermissionsByClientId().get("restricted-client");
            assertTrue(loaded.isGrantAll(), "released full set is promoted at schema version " + schemaVersion);
            assertTrue(store.hadPromotedEntriesDuringLoad(), "and written back at schema version " + schemaVersion);
        }
    }

    @Test
    void legacyGrantStampedByABuildThatDidNotKnowTheReleasedShapesIsStillPromoted() {
        // The builds between grantAll and the released-set rule (the base of this change) promote
        // only their own running set, yet stamp permissionsSchemaVersion on every persist. A client
        // paired on v2.1.9 whose node ran such a build has its 10-permission entry sitting under
        // version 1 as soon as anything persisted the store: a new pairing, a revoke, or the
        // write-back for a promoted sibling. Gating the released-set branch on the stamp would
        // leave that client frozen for good, so the stamp must not be consulted.
        Set<Permission> asShippedInV2_1_9 = AS_SHIPPED_IN_V2_1_12.stream()
                .filter(permission -> permission != Permission.MOBILE_DEVICES)
                .collect(Collectors.toSet());
        bisq.api.protobuf.PermissionSet legacyGrant = bisq.api.protobuf.PermissionSet.newBuilder()
                .addAllPermissions(asShippedInV2_1_9.stream().map(Permission::toProtoEnum).toList())
                .build();
        bisq.api.protobuf.ApiAccessStore proto = bisq.api.protobuf.ApiAccessStore.newBuilder()
                .putPermissionsByClientId("legacy-client", legacyGrant)
                .setPermissionsSchemaVersion(1)
                .build();

        ApiAccessStore store = ApiAccessStore.fromProto(proto);

        assertTrue(store.getPermissionsByClientId().get("legacy-client").isGrantAll());
        assertTrue(store.hadPromotedEntriesDuringLoad());
    }

    @Test
    void subsetEntryIsNeverPromotedRegardlessOfSchemaVersion() {
        // The schema version cannot gate promotion: a pre-grantAll node rewrites the store
        // from its own model and drops the version field, so it does not survive a downgrade.
        // A subset must stay a subset whatever version stamp the store carries.
        bisq.api.protobuf.PermissionSet subset = bisq.api.protobuf.PermissionSet.newBuilder()
                .addPermissions(Permission.OFFERBOOK.toProtoEnum())
                .build();
        for (int schemaVersion : new int[]{0, 1}) {
            bisq.api.protobuf.ApiAccessStore proto = bisq.api.protobuf.ApiAccessStore.newBuilder()
                    .putPermissionsByClientId("restricted-client", subset)
                    .setPermissionsSchemaVersion(schemaVersion)
                    .build();

            PermissionSet loaded = ApiAccessStore.fromProto(proto).getPermissionsByClientId().get("restricted-client");

            assertFalse(loaded.isGrantAll(), "subset must not be promoted at schema version " + schemaVersion);
            assertEquals(Set.of(Permission.OFFERBOOK), loaded.getPermissions());
        }
    }

    @Test
    void downgradeRewriteCycleDoesNotEscalateRestrictedEntries() {
        // new -> old -> new: a store written by this version, then rewritten by a pre-grantAll
        // node (which rebuilds the proto from its own model: grantAll entries were loaded as
        // their expanded permission list, and the grantAll + schemaVersion fields are dropped),
        // then loaded by this version again.
        ApiAccessStore newStore = new ApiAccessStore();
        newStore.getPermissionsByClientId().put("full-client", PermissionSet.grantAll());
        newStore.getPermissionsByClientId().put("restricted-client", new PermissionSet(Set.of(Permission.OFFERBOOK)));

        bisq.api.protobuf.ApiAccessStore v1Proto = newStore.toProto(false);

        // Simulate the pre-grantAll node's rewrite: only the expanded permission lists survive.
        bisq.api.protobuf.ApiAccessStore.Builder oldNodeRewrite = bisq.api.protobuf.ApiAccessStore.newBuilder();
        v1Proto.getPermissionsByClientIdMap().forEach((clientId, entry) ->
                oldNodeRewrite.putPermissionsByClientId(clientId,
                        bisq.api.protobuf.PermissionSet.newBuilder()
                                .addAllPermissions(entry.getPermissionsList())
                                .build()));

        ApiAccessStore reloaded = ApiAccessStore.fromProto(oldNodeRewrite.build());

        // The former grantAll entry serialized its full expansion, so the evidence rule
        // re-promotes it; the restricted entry must come back exactly as it was.
        assertTrue(reloaded.getPermissionsByClientId().get("full-client").isGrantAll());
        PermissionSet restricted = reloaded.getPermissionsByClientId().get("restricted-client");
        assertFalse(restricted.isGrantAll());
        assertEquals(Set.of(Permission.OFFERBOOK), restricted.getPermissions());
    }

    @Test
    void fullGrantRewrittenByAnOlderNodeThatKnowsFewerPermissionsIsStillPromoted() {
        // The rewrite the test above does not model: an old node drops every enum value it cannot
        // resolve (ProtobufUtils#fromProtoEnumStream) before rebuilding the list, so what comes
        // back is that release's own full set, not this version's expansion. The same shape is on
        // disk for a client that paired on that release and never re-paired, since a grant is only
        // rewritten at pairing time. Both v2.1.9 (10 permissions) and v2.1.10-v2.1.12 (11) shipped,
        // so both shapes must be promoted or that client is frozen out of every standard permission
        // added since.
        // Keeps the v2.1.12 row from going vacuous through the running-set branch.
        assertNotEquals(Permission.autoGrantable(), AS_SHIPPED_IN_V2_1_12);
        ApiAccessStore newStore = new ApiAccessStore();
        newStore.getPermissionsByClientId().put("full-client", PermissionSet.grantAll());
        bisq.api.protobuf.ApiAccessStore currentProto = newStore.toProto(false);

        Map<String, Permission> lastPermissionKnownByRelease = Map.of(
                "v2.1.9", Permission.USER_PROFILES,
                "v2.1.12", Permission.MOBILE_DEVICES);
        lastPermissionKnownByRelease.forEach((release, lastKnown) -> {
            bisq.api.protobuf.ApiAccessStore.Builder oldNodeRewrite = bisq.api.protobuf.ApiAccessStore.newBuilder();
            currentProto.getPermissionsByClientIdMap().forEach((clientId, entry) ->
                    oldNodeRewrite.putPermissionsByClientId(clientId,
                            bisq.api.protobuf.PermissionSet.newBuilder()
                                    .addAllPermissions(entry.getPermissionsList().stream()
                                            .filter(p -> p.getNumber() <= lastKnown.toProtoEnum().getNumber())
                                            .toList())
                                    .build()));

            ApiAccessStore reloaded = ApiAccessStore.fromProto(oldNodeRewrite.build());

            assertTrue(reloaded.getPermissionsByClientId().get("full-client").isGrantAll(),
                    "full grant rewritten by " + release + " must be promoted again");
            assertTrue(reloaded.hadPromotedEntriesDuringLoad(),
                    "and written back so the store stops depending on the rule");
        });
    }

    @Test
    void everyReleasedFullSetHoldsOnlyStandardPermissions() {
        // Promotion swaps the explicit list for the grantAll expansion, which never covers a
        // sensitive permission. The running-set branch cannot meet one by construction; the
        // released sets are a value list, so a listed permission reclassified as sensitive would
        // still match and be silently dropped from the grant. Prune it from the sets first.
        for (Set<Permission> releasedSet : ApiAccessStore.RELEASED_FULL_STANDARD_SETS) {
            for (Permission permission : releasedSet) {
                assertTrue(permission.isAutoGrantable(),
                        permission + " is listed as part of a released full set but is no longer standard");
            }
        }
    }

    @Test
    void grantAllEntrySurvivesStoreRoundTrip() {
        ApiAccessStore store = new ApiAccessStore();
        store.getPermissionsByClientId().put("client-1", PermissionSet.grantAll());

        bisq.api.protobuf.ApiAccessStore proto = store.toProto(false);
        ApiAccessStore reloaded = ApiAccessStore.fromProto(proto);

        assertEquals(1, proto.getPermissionsSchemaVersion());
        PermissionSet reloadedSet = reloaded.getPermissionsByClientId().get("client-1");
        assertTrue(reloadedSet.isGrantAll());
        assertEquals(Permission.autoGrantable(), reloadedSet.getPermissions());
    }

    @Test
    void explicitSubsetSurvivesStoreRoundTrip() {
        ApiAccessStore store = new ApiAccessStore();
        store.getPermissionsByClientId().put("client-1", new PermissionSet(Set.of(Permission.MARKET_PRICE)));

        ApiAccessStore reloaded = ApiAccessStore.fromProto(store.toProto(false));

        PermissionSet reloadedSet = reloaded.getPermissionsByClientId().get("client-1");
        assertFalse(reloadedSet.isGrantAll());
        assertEquals(Set.of(Permission.MARKET_PRICE), reloadedSet.getPermissions());
    }
}
