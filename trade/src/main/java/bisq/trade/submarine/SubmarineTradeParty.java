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

package bisq.trade.submarine;

import bisq.network.identity.NetworkId;
import bisq.trade.TradeParty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public class SubmarineTradeParty extends TradeParty {

    public SubmarineTradeParty(NetworkId networkId) {
        super(networkId);
    }

    @Override
    public bisq.trade.protobuf.TradeParty.Builder getBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.SubmarineTradeParty.Builder builder = bisq.trade.protobuf.SubmarineTradeParty.newBuilder();
        return getTradePartyBuilder(serializeForHash).setSubmarineTradeParty(builder);
    }

    public static SubmarineTradeParty fromProto(bisq.trade.protobuf.TradeParty proto) {
        return new SubmarineTradeParty(NetworkId.fromProto(proto.getNetworkId()));
    }
}