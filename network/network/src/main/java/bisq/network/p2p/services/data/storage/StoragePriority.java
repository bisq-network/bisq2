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

package bisq.network.p2p.services.data.storage;

import lombok.Getter;

/**
 * The priority which a storable type declares in its MetaData. A responder adds the requests of a store to an
 * inventory response in the order of their priority until the next one does not fit into the size limit, so the
 * entries of a type with a lower priority are the ones a truncated response leaves out (rule 13 of
 * docs/specifications/network/inventory-hash-set-filter.md).
 * <p>
 * The set is closed. The priority is a policy of the type, shared by every node which runs the build, so a new value
 * needs a new constant here rather than a number at a call site.
 * <p>
 * It covers the declared priority only. The field of MetaData stays an int, because remove requests, refresh requests
 * and mailbox data carry a MetaData which a sender or a relay filled, and {@code MetaData.fromProto} accepts any value
 * from it. A node applies only the priority which it registered for the class name (rule 4 of
 * docs/specifications/network/storage-metadata.md).
 */
public enum StoragePriority {
    LOW(-1),
    DEFAULT(0),
    HIGH(1),
    HIGHEST(2);

    @Getter
    private final int value;

    StoragePriority(int value) {
        this.value = value;
    }
}
