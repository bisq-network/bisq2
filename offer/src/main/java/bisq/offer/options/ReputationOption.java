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

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode
public final class ReputationOption implements OfferOption {
    private final Set<ReputationProof> reputationProofs;

    public ReputationOption(Set<ReputationProof> reputationProofs) {
        this.reputationProofs = reputationProofs;
    }

    public bisq.offer.protobuf.OfferOption toProto() {
        return getOfferOptionBuilder().setReputationOption(
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
