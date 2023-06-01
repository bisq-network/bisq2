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

package bisq.protocol.poc.bisq_easy;

import bisq.common.proto.ProtoEnum;
import bisq.common.util.ProtobufUtils;

public enum BisqEasyTradePhase implements ProtoEnum {
    NEGOTIATION,
    FIAT_TRANSFER,
    BTC_TRANSFER,
    COMPLETED;

    @Override
    public bisq.protocol.protobuf.BisqEasyTradePhase toProto() {
        return bisq.protocol.protobuf.BisqEasyTradePhase.valueOf(name());
    }

    public static BisqEasyTradePhase fromProto(bisq.protocol.protobuf.BisqEasyTradePhase proto) {
        return ProtobufUtils.enumFromProto(BisqEasyTradePhase.class, proto.name());
    }
}

