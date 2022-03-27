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

import bisq.common.util.ProtobufUtils;

// Data for verifying fee payment. Open question how we deal with fees...
public record FeeOption(FeeType feeType, int blockHeightAtFeePayment, String feeTxId) implements OfferOption {
    public enum FeeType {
        BTC,
        BSQ;

        public bisq.offer.protobuf.FeeOption.FeeType toProto() {
            return bisq.offer.protobuf.FeeOption.FeeType.valueOf(name());
        }

        public static FeeType fromProto(bisq.offer.protobuf.FeeOption.FeeType proto) {
            return ProtobufUtils.enumFromProto(FeeType.class, proto.name());
        }
    }

    public bisq.offer.protobuf.OfferOption toProto() {
        return getOfferOptionBuilder().setFeeOption(bisq.offer.protobuf.FeeOption.newBuilder()
                        .setFeeType(feeType.toProto())
                        .setBlockHeightAtFeePayment(blockHeightAtFeePayment)
                        .setFeeTxId(feeTxId))
                .build();
    }

    public static FeeOption fromProto(bisq.offer.protobuf.FeeOption proto) {
        return new FeeOption(FeeType.fromProto(proto.getFeeType()),
                proto.getBlockHeightAtFeePayment(),
                proto.getFeeTxId());
    }
}
//  Bisq 1
//    private String offerFeePaymentTxId;
//    private final long blockHeightAtOfferCreation;
//    private final long txFee;
//    private final long makerFee;
//    private final boolean isCurrencyForMakerFeeBtc;
