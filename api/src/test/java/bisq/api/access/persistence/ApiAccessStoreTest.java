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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAccessStoreTest {
    @Test
    void fullPermissionSetIsPromotedToGrantAllOnLoad() {
        // An entry already covering every permission of the running version is promoted to
        // grantAll: promotion grants nothing extra at that moment, it only keeps the grant
        // covering permissions added later — which is what every full grant was issued to mean.
        // This is how pre-grantAll stores (always full grants) gain forward coverage.
        bisq.api.protobuf.PermissionSet fullEntry = bisq.api.protobuf.PermissionSet.newBuilder()
                .addAllPermissions(Permission.autoGrantable().stream().map(Permission::toProtoEnum).toList())
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
