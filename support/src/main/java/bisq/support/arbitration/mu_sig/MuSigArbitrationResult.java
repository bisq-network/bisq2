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

package bisq.support.arbitration.mu_sig;

import bisq.common.proto.NetworkProto;
import bisq.common.proto.PersistableProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.support.arbitration.ArbitrationPayoutDistributionType;
import bisq.support.arbitration.ArbitrationResultReason;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.System.currentTimeMillis;

@Getter
public class MuSigArbitrationResult implements NetworkProto, PersistableProto {
    public static final int MAX_SUMMARY_NOTES_LENGTH = 1_000;

    private final long date;
    private final byte[] contractHash;
    private final ArbitrationResultReason arbitrationResultReason;
    private final ArbitrationPayoutDistributionType arbitrationPayoutDistributionType;
    private final long buyerPayoutAmount;
    private final long sellerPayoutAmount;
    private final Optional<String> summaryNotes;

    public MuSigArbitrationResult(byte[] contractHash,
                                  ArbitrationResultReason arbitrationResultReason,
                                  ArbitrationPayoutDistributionType arbitrationPayoutDistributionType,
                                  long buyerPayoutAmount,
                                  long sellerPayoutAmount,
                                  Optional<String> summaryNotes) {
        this(currentTimeMillis(),
                contractHash,
                arbitrationResultReason,
                arbitrationPayoutDistributionType,
                buyerPayoutAmount,
                sellerPayoutAmount,
                summaryNotes);
    }

    private MuSigArbitrationResult(long date,
                                   byte[] contractHash,
                                   ArbitrationResultReason arbitrationResultReason,
                                   ArbitrationPayoutDistributionType arbitrationPayoutDistributionType,
                                   long buyerPayoutAmount,
                                   long sellerPayoutAmount,
                                   Optional<String> summaryNotes) {
        this.date = date;
        this.contractHash = contractHash.clone();
        this.arbitrationResultReason = arbitrationResultReason;
        this.arbitrationPayoutDistributionType = arbitrationPayoutDistributionType;
        this.buyerPayoutAmount = buyerPayoutAmount;
        this.sellerPayoutAmount = sellerPayoutAmount;
        this.summaryNotes = summaryNotes;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateDate(date);
        NetworkDataValidation.validateHash(contractHash);
        checkArgument(arbitrationResultReason != null, "arbitrationResultReason must not be null");
        checkArgument(arbitrationPayoutDistributionType != null, "arbitrationPayoutDistributionType must not be null");
        checkArgument(buyerPayoutAmount >= 0, "buyerPayoutAmount must not be negative");
        checkArgument(sellerPayoutAmount >= 0, "sellerPayoutAmount must not be negative");
        NetworkDataValidation.validateText(summaryNotes, MAX_SUMMARY_NOTES_LENGTH);
    }

    @Override
    public bisq.support.protobuf.MuSigArbitrationResult.Builder getBuilder(boolean serializeForHash) {
        var builder = bisq.support.protobuf.MuSigArbitrationResult.newBuilder()
                .setDate(date)
                .setContractHash(ByteString.copyFrom(contractHash))
                .setArbitrationResultReason(arbitrationResultReason.toProtoEnum())
                .setArbitrationPayoutDistributionType(arbitrationPayoutDistributionType.toProtoEnum())
                .setBuyerPayoutAmount(buyerPayoutAmount)
                .setSellerPayoutAmount(sellerPayoutAmount);
        summaryNotes.ifPresent(builder::setSummaryNotes);
        return builder;
    }

    @Override
    public bisq.support.protobuf.MuSigArbitrationResult toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigArbitrationResult fromProto(bisq.support.protobuf.MuSigArbitrationResult proto) {
        return new MuSigArbitrationResult(
                proto.getDate(),
                proto.getContractHash().toByteArray(),
                ArbitrationResultReason.fromProto(proto.getArbitrationResultReason()),
                ArbitrationPayoutDistributionType.fromProto(proto.getArbitrationPayoutDistributionType()),
                proto.getBuyerPayoutAmount(),
                proto.getSellerPayoutAmount(),
                proto.hasSummaryNotes() ? Optional.of(proto.getSummaryNotes()) : Optional.empty());
    }

    public byte[] getContractHash() {
        return contractHash.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MuSigArbitrationResult that)) {
            return false;
        }
        return date == that.date &&
                Arrays.equals(contractHash, that.contractHash) &&
                arbitrationResultReason == that.arbitrationResultReason &&
                arbitrationPayoutDistributionType == that.arbitrationPayoutDistributionType &&
                buyerPayoutAmount == that.buyerPayoutAmount &&
                sellerPayoutAmount == that.sellerPayoutAmount &&
                Objects.equals(summaryNotes, that.summaryNotes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(date,
                arbitrationResultReason,
                arbitrationPayoutDistributionType,
                buyerPayoutAmount,
                sellerPayoutAmount,
                summaryNotes);
        result = 31 * result + Arrays.hashCode(contractHash);
        return result;
    }
}
