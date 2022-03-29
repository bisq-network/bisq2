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
import bisq.common.proto.UnresolvableProtobufMessageException;

public interface OfferOption extends Proto {
    bisq.offer.protobuf.OfferOption toProto();

    default bisq.offer.protobuf.OfferOption.Builder getOfferOptionBuilder() {
        return bisq.offer.protobuf.OfferOption.newBuilder();
    }

    static OfferOption fromProto(bisq.offer.protobuf.OfferOption proto) {
        switch (proto.getMessageCase()) {
            case AMOUNTOPTION -> {
                AmountOption.fromProto(proto.getAmountOption());
            }
            case COLLATERALOPTION -> {
                CollateralOption.fromProto(proto.getCollateralOption());
            }
            case CONTRACTOPTION -> {
                ContractOption.fromProto(proto.getContractOption());
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
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}
