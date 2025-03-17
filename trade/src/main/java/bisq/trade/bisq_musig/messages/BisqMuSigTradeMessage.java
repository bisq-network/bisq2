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

package bisq.trade.bisq_musig.messages;

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.identity.NetworkId;
import bisq.trade.protocol.messages.TradeMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class BisqMuSigTradeMessage extends TradeMessage {
    protected BisqMuSigTradeMessage(String id, String tradeId, String protocolVersion, NetworkId sender, NetworkId receiver) {
        super(id, tradeId, protocolVersion, sender, receiver);
    }

    public static BisqMuSigTradeMessage fromProto(bisq.trade.protobuf.BisqMuSigTradeMessage proto) {
       /* switch (proto.getMessageCase()) {

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }*/
        throw new UnresolvableProtobufMessageException(proto);
    }
}