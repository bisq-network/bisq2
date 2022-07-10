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

package bisq.social.user.proof;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class ProofOfBurnProof implements Proof {
    private final String txId;
    private final long burntAmount;
    private final long date;

    public ProofOfBurnProof(String txId, long burntAmount, long date) {
        this.txId = txId;
        this.burntAmount = burntAmount;
        this.date = date;
    }

    @Override
    public bisq.user.protobuf.Proof toProto() {
        return getProofBuilder().setProofOfBurnProof(
                        bisq.user.protobuf.ProofOfBurnProof.newBuilder()
                                .setTxId(txId)
                                .setBurntAmount(burntAmount)
                                .setDate(date))
                .build();
    }

    public static ProofOfBurnProof fromProto(bisq.user.protobuf.ProofOfBurnProof proto) {
        return new ProofOfBurnProof(proto.getTxId(), proto.getBurntAmount(), proto.getDate());
    }
}