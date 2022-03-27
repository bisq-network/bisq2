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

public interface ReputationProof {
    bisq.offer.protobuf.ReputationProof toProto();

    default bisq.offer.protobuf.ReputationProof.Builder getReputationProofBuilder() {
        return bisq.offer.protobuf.ReputationProof.newBuilder();
    }

    static ReputationProof fromProto(bisq.offer.protobuf.ReputationProof proto) {
        switch (proto.getMessageCase()) {
            case ACCOUNTCREATIONDATEPROOF -> {
                return AccountCreationDateProof.fromProto(proto.getAccountCreationDateProof());
            }
            case MESSAGE_NOT_SET -> {
                throw new RuntimeException("MESSAGE_NOT_SET. networkMessage.getMessageCase()=" + proto.getMessageCase());
            }
        }
        throw new RuntimeException("Could not resolve message case. networkMessage.getMessageCase()=" + proto.getMessageCase());
    }
}
