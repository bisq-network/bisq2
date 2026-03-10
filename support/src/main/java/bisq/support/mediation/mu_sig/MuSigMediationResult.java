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

import bisq.common.proto.NetworkProto;
import bisq.common.proto.PersistableProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.MediationResultReason;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.System.currentTimeMillis;

@Getter
@EqualsAndHashCode
public class MuSigMediationResult implements NetworkProto, PersistableProto {
    public static final int MAX_SUMMARY_NOTES_LENGTH = 1_000;

    private final long date;
    private final MediationResultReason mediationResultReason;
    private final MediationPayoutDistributionType mediationPayoutDistributionType;
    private final Optional<Long> proposedBuyerPayoutAmount;
    private final Optional<Long> proposedSellerPayoutAmount;
    private final Optional<Double> payoutAdjustmentPercentage;
    private final Optional<String> summaryNotes;

    public MuSigMediationResult(MediationResultReason mediationResultReason,
                                MediationPayoutDistributionType mediationPayoutDistributionType,
                                Optional<Long> proposedBuyerPayoutAmount,
                                Optional<Long> proposedSellerPayoutAmount,
                                Optional<Double> payoutAdjustmentPercentage,
                                Optional<String> summaryNotes) {
        this(currentTimeMillis(),
                mediationResultReason,
                mediationPayoutDistributionType,
                proposedBuyerPayoutAmount,
                proposedSellerPayoutAmount,
                payoutAdjustmentPercentage,
                summaryNotes);
    }

    private MuSigMediationResult(long date,
                                 MediationResultReason mediationResultReason,
                                 MediationPayoutDistributionType mediationPayoutDistributionType,
                                 Optional<Long> proposedBuyerPayoutAmount,
                                 Optional<Long> proposedSellerPayoutAmount,
                                 Optional<Double> payoutAdjustmentPercentage,
                                 Optional<String> summaryNotes) {
        this.date = date;
        this.mediationResultReason = mediationResultReason;
        this.mediationPayoutDistributionType = mediationPayoutDistributionType;
        this.proposedBuyerPayoutAmount = proposedBuyerPayoutAmount;
        this.proposedSellerPayoutAmount = proposedSellerPayoutAmount;
        this.payoutAdjustmentPercentage = payoutAdjustmentPercentage;
        this.summaryNotes = summaryNotes;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateDate(date);
        checkArgument(mediationResultReason != null, "mediationResultReason must not be null");
        checkArgument(mediationPayoutDistributionType != null, "mediationPayoutDistributionType must not be null");
        boolean noPayout = mediationPayoutDistributionType == MediationPayoutDistributionType.NO_PAYOUT;
        checkArgument(noPayout
                ? proposedBuyerPayoutAmount.isEmpty() && proposedSellerPayoutAmount.isEmpty()
                : proposedBuyerPayoutAmount.isPresent() && proposedSellerPayoutAmount.isPresent(),
                "payout amounts must be present for payout distributions and absent for NO_PAYOUT");
        proposedBuyerPayoutAmount.ifPresent(value ->
                checkArgument(value >= 0, "proposedBuyerPayoutAmount must not be negative"));
        proposedSellerPayoutAmount.ifPresent(value ->
                checkArgument(value >= 0, "proposedSellerPayoutAmount must not be negative"));
        NetworkDataValidation.validateText(summaryNotes, MAX_SUMMARY_NOTES_LENGTH);
    }

    @Override
    public bisq.support.protobuf.MuSigMediationResult.Builder getBuilder(boolean serializeForHash) {
        var builder = bisq.support.protobuf.MuSigMediationResult.newBuilder()
                .setDate(date)
                .setMediationResultReason(mediationResultReason.toProtoEnum())
                .setMediationPayoutDistributionType(mediationPayoutDistributionType.toProtoEnum());
        proposedBuyerPayoutAmount.ifPresent(builder::setProposedBuyerPayoutAmount);
        proposedSellerPayoutAmount.ifPresent(builder::setProposedSellerPayoutAmount);
        payoutAdjustmentPercentage.ifPresent(builder::setPayoutAdjustmentPercentage);
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
                MediationPayoutDistributionType.fromProto(proto.getMediationPayoutDistributionType()),
                proto.hasProposedBuyerPayoutAmount() ? Optional.of(proto.getProposedBuyerPayoutAmount()) : Optional.empty(),
                proto.hasProposedSellerPayoutAmount() ? Optional.of(proto.getProposedSellerPayoutAmount()) : Optional.empty(),
                proto.hasPayoutAdjustmentPercentage() ? Optional.of(proto.getPayoutAdjustmentPercentage()) : Optional.empty(),
                proto.hasSummaryNotes() ? Optional.of(proto.getSummaryNotes()) : Optional.empty());
    }
}
