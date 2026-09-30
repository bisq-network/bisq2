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

package bisq.support.mediation;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;

public enum MediationPayoutDistributionType implements ProtoEnum {
    CUSTOM_PAYOUT,
    BUYER_GETS_TRADE_AMOUNT,
    BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
    BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
    SELLER_GETS_TRADE_AMOUNT,
    SELLER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
    SELLER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
    NO_PAYOUT;

    @Override
    public bisq.support.protobuf.MediationPayoutDistributionType toProtoEnum() {
        return bisq.support.protobuf.MediationPayoutDistributionType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static MediationPayoutDistributionType fromProto(bisq.support.protobuf.MediationPayoutDistributionType proto) {
        return ProtobufUtils.enumFromProto(MediationPayoutDistributionType.class, proto.name(), CUSTOM_PAYOUT);
    }
}
