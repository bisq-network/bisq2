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

import bisq.common.proto.ProtoEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The id must not be changed as it is used for the serialisation.
 * <p>
 * {@code autoGrantable} encodes the standard-vs-sensitive distinction: a grantAll grant expands
 * only to auto-grantable permissions (see {@link PermissionSet}), so already-paired clients gain
 * future NORMAL features automatically but can never silently acquire a security-sensitive
 * capability.
 * <p>
 * SECURE BY DEFAULT: the single-argument constructor marks a permission SENSITIVE
 * ({@code autoGrantable = false}). A new permission is therefore excluded from grantAll unless it
 * explicitly opts in via {@code STANDARD}, making "is this safe to auto-grant?" a conscious
 * decision at declaration time. Forgetting to decide fails closed — the permission requires an
 * explicit per-device grant rather than silently joining grantAll.
 */
public enum Permission implements ProtoEnum {
    TRADE_CHAT_CHANNELS(0, Kind.STANDARD),
    EXPLORER(1, Kind.STANDARD),
    MARKET_PRICE(2, Kind.STANDARD),
    OFFERBOOK(3, Kind.STANDARD),
    PAYMENT_ACCOUNTS(4, Kind.STANDARD),
    REPUTATION(5, Kind.STANDARD),
    SETTINGS(6, Kind.STANDARD),
    TRADES(7, Kind.STANDARD),
    USER_IDENTITIES(8, Kind.STANDARD),
    USER_PROFILES(9, Kind.STANDARD),
    MOBILE_DEVICES(10, Kind.STANDARD),
    // Id 11 is reserved for PRIVATE_CHAT_CHANNELS, which is declared on the for-mobile branch.
    // Ids are wire values, so the gap stays until that work reaches main.
    //
    // NETWORK_INFO has no REST route of its own — the network state is served over the
    // NETWORK_INFO subscription only. It is declared here because the permission names the data,
    // not the transport: without it that data is the one thing in the API reachable with no grant
    // at all, and it is not diagnostics — NetworkInfoDto carries the node's own address, its keyId
    // and the address of every peer it is connected to.
    //
    // STANDARD so a grantAll pairing picks it up at read time (see PermissionSet) rather than
    // losing the network banner until the user pairs again.
    NETWORK_INFO(12, Kind.STANDARD);

    /** Grant classification. Named so a declaration reads as an explicit security decision. */
    public enum Kind {
        /** Auto-grantable: covered by grantAll, gained automatically by already-paired clients. */
        STANDARD,
        /** Security-sensitive: never covered by grantAll, always needs an explicit per-device grant. */
        SENSITIVE
    }

    @Getter
    private final int id;
    private final Kind kind;

    // Sensitive by default: an id-only declaration is treated as SENSITIVE so a forgotten
    // classification fails closed. Standard permissions must opt in explicitly with Kind.STANDARD.
    Permission(int id) {
        this(id, Kind.SENSITIVE);
    }

    Permission(int id, Kind kind) {
        this.id = id;
        this.kind = kind;
    }

    public boolean isAutoGrantable() {
        return kind == Kind.STANDARD;
    }

    /**
     * All permissions a grantAll grant expands to — the "standard" set. Sensitive permissions
     * (autoGrantable = false) are excluded by construction and must be granted explicitly.
     * <p>
     * Ordered by {@code id} into an insertion-ordered set: this feeds the grantAll expansion
     * serialized by {@code PermissionSet.getBuilder}, and an unspecified iteration order (as a
     * plain immutable Set has) would make the proto's repeated field non-deterministic across
     * runs, breaking serializeForHash stability. Set semantics are unchanged — equality stays
     * order-independent, so promotion/folding comparisons are unaffected.
     */
    public static Set<Permission> autoGrantable() {
        // Unmodifiable AND insertion-ordered: PermissionSet caches this in a static field and
        // returns it directly from getPermissions(), so a mutable set would let any caller alter
        // the grantAll expansion for every client (and, once sensitive permissions exist, add
        // one). LinkedHashSet keeps the id order needed for serializeForHash determinism.
        LinkedHashSet<Permission> ordered = Arrays.stream(values())
                .filter(Permission::isAutoGrantable)
                .sorted(Comparator.comparingInt(Permission::getId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(ordered);
    }

    @Override
    public bisq.api.protobuf.Permission toProtoEnum() {
        return bisq.api.protobuf.Permission.valueOf(getProtobufEnumPrefix() + name());
    }

    // fromProto is not used

    public static Permission fromId(int id) {
        for (Permission permission : values()) {
            if (permission.id == id) return permission;
        }
        throw new IllegalArgumentException("No permission found for id " + id);
    }

}
