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

import bisq.common.proto.Proto;

public interface ListingOption extends Proto {
    bisq.offer.protobuf.ListingOption toProto();

    default bisq.offer.protobuf.ListingOption.Builder getListingOptionBuilder() {
        return bisq.offer.protobuf.ListingOption.newBuilder();
    }

    static ListingOption fromProto(bisq.offer.protobuf.ListingOption proto) {
        switch (proto.getMessageCase()) {
            case AMOUNTOPTION -> {
                AmountOption.fromProto(proto.getAmountOption());
            }
            case COLLATERALOPTIONS -> {
                CollateralOptions.fromProto(proto.getCollateralOptions());
            }
            case CONTRACTOPTIONS -> {
                ContractOptions.fromProto(proto.getContractOptions());
            }
            case FEEOPTION -> {
                FeeOption.fromProto(proto.getFeeOption());
            }
            case FIATSETTLEMENTOPTION -> {
                FiatSettlementOption.fromProto(proto.getFiatSettlementOption());
            }
            case REPUTATIONOPTION -> {
                ReputationOption.fromProto(proto.getReputationOption());
            }
            case SUPPORTOPTION -> {
                SupportOption.fromProto(proto.getSupportOption());
            }
            case MESSAGE_NOT_SET -> {
                throw new RuntimeException("MESSAGE_NOT_SET. networkMessage.getMessageCase()=" + proto.getMessageCase());
            }
        }
        throw new RuntimeException("Could not resolve message case. networkMessage.getMessageCase()=" + proto.getMessageCase());
    }
}
