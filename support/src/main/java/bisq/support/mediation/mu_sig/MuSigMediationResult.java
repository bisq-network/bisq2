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

package bisq.support.mediation.mu_sig;

import bisq.common.proto.PersistableProto;
import bisq.support.mediation.MediationResultReason;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@Getter
@EqualsAndHashCode
public class MuSigMediationResult implements PersistableProto {

    private final long date;
    private final MediationResultReason mediationResultReason;
    private final long proposedBuyerPayoutAmount;
    private final long proposedSellerPayoutAmount;
    private final Optional<String> summaryNotes;

    public MuSigMediationResult(MediationResultReason mediationResultReason,
                                long proposedBuyerPayoutAmount,
                                long proposedSellerPayoutAmount,
                                Optional<String> summaryNotes) {
        this(currentTimeMillis(), mediationResultReason, proposedBuyerPayoutAmount, proposedSellerPayoutAmount, summaryNotes);
    }

    private MuSigMediationResult(long date,
                                MediationResultReason mediationResultReason,
                                long proposedBuyerPayoutAmount,
                                long proposedSellerPayoutAmount,
                                Optional<String> summaryNotes) {
        this.date = date;
        this.mediationResultReason = mediationResultReason;
        this.proposedBuyerPayoutAmount = proposedBuyerPayoutAmount;
        this.proposedSellerPayoutAmount = proposedSellerPayoutAmount;
        this.summaryNotes = summaryNotes;
    }

    @Override
    public bisq.support.protobuf.MuSigMediationResult.Builder getBuilder(boolean serializeForHash) {
        var builder = bisq.support.protobuf.MuSigMediationResult.newBuilder()
                .setDate(date)
                .setMediationResultReason(mediationResultReason.toProtoEnum())
                .setProposedBuyerPayoutAmount(proposedBuyerPayoutAmount)
                .setProposedSellerPayoutAmount(proposedSellerPayoutAmount);
        summaryNotes.ifPresent(builder::setSummaryNotes);
        return builder;
    }


    @Override
    public bisq.support.protobuf.MuSigMediationResult toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigMediationResult fromProto(bisq.support.protobuf.MuSigMediationResult proto) {
        return new MuSigMediationResult(
                proto.getDate(),
                MediationResultReason.fromProto(proto.getMediationResultReason()),
                proto.getProposedBuyerPayoutAmount(),
                proto.getProposedSellerPayoutAmount(),
                proto.hasSummaryNotes() ? Optional.of(proto.getSummaryNotes()) : Optional.empty());
    }
}
