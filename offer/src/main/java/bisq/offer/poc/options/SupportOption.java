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

package bisq.offer.poc.options;

import bisq.offer.offer_options.OfferOption;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

//todo support agent is selected at take offer time not at offer creation
@Getter
@ToString
@EqualsAndHashCode
public final class SupportOption implements OfferOption {
    private final List<DisputeAgent> disputeAgents;

    public SupportOption(List<DisputeAgent> disputeAgents) {
        this.disputeAgents = disputeAgents;

        // All lists need to sort deterministically as the data is used in the proof of work check
        disputeAgents.sort(Comparator.comparingInt(DisputeAgent::hashCode));
    }

    public bisq.offer.protobuf.OfferOption toProto() {
        return getOfferOptionBuilder().setSupportOption(bisq.offer.protobuf.SupportOption.newBuilder()
                        .addAllDisputeAgents(disputeAgents.stream()
                                .map(DisputeAgent::toProto)
                                .collect(Collectors.toList())))
                .build();
    }

    public static SupportOption fromProto(bisq.offer.protobuf.SupportOption proto) {
        return new SupportOption(proto.getDisputeAgentsList().stream()
                .map(DisputeAgent::fromProto)
                .collect(Collectors.toList()));
    }
}
