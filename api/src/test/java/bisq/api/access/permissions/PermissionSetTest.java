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

package bisq.api.access.permissions;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionSetTest {
    @Test
    void grantAllExpandsToAllPermissionsOfTheRunningVersion() {
        PermissionSet permissionSet = PermissionSet.grantAll();

        assertTrue(permissionSet.isGrantAll());
        assertEquals(Permission.autoGrantable(), permissionSet.getPermissions());
    }

    @Test
    void explicitSetIsReturnedAsIs() {
        Set<Permission> explicit = Set.of(Permission.OFFERBOOK, Permission.TRADES);
        PermissionSet permissionSet = new PermissionSet(explicit);

        assertFalse(permissionSet.isGrantAll());
        assertEquals(explicit, permissionSet.getPermissions());
    }

    @Test
    void grantAllSurvivesProtoRoundTrip() {
        bisq.api.protobuf.PermissionSet proto = PermissionSet.grantAll().toProto(false);
        PermissionSet fromProto = PermissionSet.fromProto(proto);

        assertTrue(fromProto.isGrantAll());
        assertEquals(Permission.autoGrantable(), fromProto.getPermissions());
    }

    @Test
    void grantAllSerializesExpandedListForDowngradedNodes() {
        // A node downgraded to a version without the grantAll field only reads the explicit
        // list, so the full expansion must be present in the proto. Simulate the legacy read
        // path (ProtobufUtils.fromProtoEnumSet on the repeated field) to prove it.
        bisq.api.protobuf.PermissionSet proto = PermissionSet.grantAll().toProto(false);

        Set<Permission> legacyRead = bisq.common.proto.ProtobufUtils.fromProtoEnumSet(Permission.class, proto.getPermissionsList());
        assertEquals(Permission.autoGrantable(), legacyRead);
    }

    @Test
    void unknownEnumValuesInProtoAreDroppedNotEscalated() {
        // Version skew / corruption must shrink the effective grant, never expand it.
        bisq.api.protobuf.PermissionSet proto = bisq.api.protobuf.PermissionSet.newBuilder()
                .addPermissions(Permission.OFFERBOOK.toProtoEnum())
                .addPermissionsValue(9999)
                .build();

        PermissionSet fromProto = PermissionSet.fromProto(proto);

        assertFalse(fromProto.isGrantAll());
        assertEquals(Set.of(Permission.OFFERBOOK), fromProto.getPermissions());
    }

    @Test
    void emptyExplicitGrantSurvivesProtoRoundTrip() {
        PermissionSet empty = new PermissionSet(Set.of());
        PermissionSet fromProto = PermissionSet.fromProto(empty.toProto(false));

        assertFalse(fromProto.isGrantAll());
        assertTrue(fromProto.getPermissions().isEmpty());
    }

    @Test
    void explicitSetSurvivesProtoRoundTrip() {
        Set<Permission> explicit = Set.of(Permission.SETTINGS);
        bisq.api.protobuf.PermissionSet proto = new PermissionSet(explicit).toProto(false);
        PermissionSet fromProto = PermissionSet.fromProto(proto);

        assertFalse(fromProto.isGrantAll());
        assertEquals(explicit, fromProto.getPermissions());
    }

    @Test
    void protoPermissionListIsSortedById() {
        // Set iteration order is unspecified (Set.copyOf salts it per JVM run), so an unsorted
        // repeated field would make serializeForHash non-deterministic across runs. Using all
        // permissions makes an accidentally id-ordered iteration practically impossible.
        List<bisq.api.protobuf.Permission> expectedIdOrder = Arrays.stream(Permission.values())
                .sorted(Comparator.comparingInt(Permission::getId))
                .map(Permission::toProtoEnum)
                .toList();

        PermissionSet explicit = new PermissionSet(EnumSet.allOf(Permission.class));
        assertEquals(expectedIdOrder, explicit.toProto(true).getPermissionsList());

        List<bisq.api.protobuf.Permission> expectedGrantAllOrder = Permission.autoGrantable().stream()
                .sorted(Comparator.comparingInt(Permission::getId))
                .map(Permission::toProtoEnum)
                .toList();
        assertEquals(expectedGrantAllOrder, PermissionSet.grantAll().toProto(true).getPermissionsList());
    }

    @Test
    void legacyProtoWithoutGrantAllFieldYieldsExplicitSet() {
        // Simulates a store entry written before the grantAll field existed.
        bisq.api.protobuf.PermissionSet legacyProto = bisq.api.protobuf.PermissionSet.newBuilder()
                .addAllPermissions(Set.of(Permission.OFFERBOOK).stream().map(Permission::toProtoEnum).toList())
                .build();

        PermissionSet fromProto = PermissionSet.fromProto(legacyProto);

        assertFalse(fromProto.isGrantAll());
        assertEquals(Set.of(Permission.OFFERBOOK), fromProto.getPermissions());
    }
}
