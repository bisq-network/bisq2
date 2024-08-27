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

package bisq.account.protocol_type;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.UnresolvableProtobufMessageException;

// TODO (refactoring) does not fit right into the account module. But dependency graph does not provide a lower level module
// where it would fit in. protocol module is rather high level and we use protocol type in the POC code in 
// the account, offer and contract modules. So leave it for now here. Maybe we need another low level exchange domain 
// specific module where it would fit in.
public interface ProtocolType extends ProtoEnum {
    static ProtocolType fromProto(bisq.account.protobuf.ProtocolType proto) {
        return switch (proto.getMessageCase()) {
            case TRADEPROTOCOLTYPE -> TradeProtocolType.fromProto(proto.getTradeProtocolType());
            case LOANPROTOCOLTYPE -> LoanProtocolType.fromProto(proto.getLoanProtocolType());
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}
