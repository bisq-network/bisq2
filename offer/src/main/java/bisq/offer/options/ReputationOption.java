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

import java.util.Set;
import java.util.stream.Collectors;

// Provides reputation proofs. E.g.Account age witness hash, signed account age witness,
// tx nodeId and signature of burned BSQ, or social media account address,...
public record ReputationOption(Set<ReputationProof> reputationProofs) implements ListingOption {
    public bisq.offer.protobuf.ListingOption toProto() {
        return getListingOptionBuilder().setReputationOption(
                bisq.offer.protobuf.ReputationOption.newBuilder()
                .addAllReputationProofs(reputationProofs.stream()
                        .map(ReputationProof::toProto)
                        .collect(Collectors.toList())))
                .build();
    }

    public static ReputationOption fromProto(bisq.offer.protobuf.ReputationOption proto) {
        return new ReputationOption(proto.getReputationProofsList().stream()
                .map(ReputationProof::fromProto)
                .collect(Collectors.toSet()));
    }
}
