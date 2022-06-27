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

package bisq.offer.options;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

// Notarizing account creation date with Open time stamp
@Getter
@ToString
@EqualsAndHashCode
public final class AccountCreationDateProof implements ReputationProof {
    private final String hashOfAccount;
    private final String otsProof;

    public AccountCreationDateProof(String hashOfAccount, String otsProof) {
        this.hashOfAccount = hashOfAccount;
        this.otsProof = otsProof;
    }

    public bisq.offer.protobuf.ReputationProof toProto() {
        return getReputationProofBuilder().setAccountCreationDateProof(
                        bisq.offer.protobuf.AccountCreationDateProof.newBuilder()
                                .setHashOfAccount(hashOfAccount)
                                .setOtsProof(otsProof))
                .build();
    }

    public static AccountCreationDateProof fromProto(bisq.offer.protobuf.AccountCreationDateProof proto) {
        return new AccountCreationDateProof(proto.getHashOfAccount(), proto.getOtsProof());
    }
}
