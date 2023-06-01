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

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class BisqEasyProtocolModel implements Proto {
    private final BisqEasyTrade trade;
    private final BisqEasyTradePhase phase;

    public BisqEasyProtocolModel(BisqEasyTrade trade, BisqEasyTradePhase phase) {
        this.trade = trade;
        this.phase = phase;
    }

    @Override
    public bisq.protocol.protobuf.BisqEasyProtocolModel toProto() {
        return bisq.protocol.protobuf.BisqEasyProtocolModel.newBuilder()
                .setTrade(trade.toProto())
                .setPhase(phase.toProto())
                .build();
    }

    public static BisqEasyProtocolModel fromProto(bisq.protocol.protobuf.BisqEasyProtocolModel proto) {
        return new BisqEasyProtocolModel(BisqEasyTrade.fromProto(proto.getTrade()),
                BisqEasyTradePhase.fromProto(proto.getPhase()));
    }

    public String getId() {
        return trade.getId();
    }
}