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

package bisq.oracle_node.bisq1_bridge.dto.dao;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum OpReturnType {
    UNDEFINED((byte) 0x00),
    PROPOSAL((byte) 0x10),
    COMPENSATION_REQUEST((byte) 0x11),
    REIMBURSEMENT_REQUEST((byte) 0x12),
    BLIND_VOTE((byte) 0x13),
    VOTE_REVEAL((byte) 0x14),
    LOCKUP((byte) 0x15),
    ASSET_LISTING_FEE((byte) 0x16),
    PROOF_OF_BURN((byte) 0x17);

    @Getter
    private final byte type;

    OpReturnType(byte type) {
        this.type = type;
    }

    public static Optional<OpReturnType> getOpReturnType(byte type) {
        return Arrays.stream(OpReturnType.values())
                .filter(opReturnType -> opReturnType.type == type)
                .findAny();
    }
}
