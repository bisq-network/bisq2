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

package bisq.user.proof;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class BondedRoleProof implements Proof {
    private final String txId;
    private final String signature;

    public BondedRoleProof(String txId, String signature) {
        this.txId = txId;
        this.signature = signature;
    }

    @Override
    public bisq.user.protobuf.Proof toProto() {
        return getProofBuilder().setBondedRoleProof(
                        bisq.user.protobuf.BondedRoleProof.newBuilder()
                                .setTxId(txId)
                                .setSignature(signature))
                .build();
    }

    public static BondedRoleProof fromProto(bisq.user.protobuf.BondedRoleProof proto) {
        return new BondedRoleProof(proto.getTxId(), proto.getSignature());
    }
}