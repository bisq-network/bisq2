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

package bisq.trade;

import bisq.common.observable.Observable;
import bisq.common.proto.PersistableProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.contract.ContractSignatureData;
import bisq.network.identity.NetworkId;
import bisq.trade.bisq_easy.BisqEasyTradeParty;
import bisq.trade.bisq_musig.BisqMuSigTradeParty;
import bisq.trade.submarine.SubmarineTradeParty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public abstract class TradeParty implements PersistableProto {
    private final NetworkId networkId;
    private final Observable<ContractSignatureData> contractSignatureData = new Observable<>();

    protected TradeParty(NetworkId networkId) {
        this.networkId = networkId;
    }

    @Override
    public bisq.trade.protobuf.TradeParty toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public bisq.trade.protobuf.TradeParty.Builder getTradePartyBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.TradeParty.Builder builder = bisq.trade.protobuf.TradeParty.newBuilder()
                .setNetworkId(networkId.toProto(serializeForHash));
        Optional.ofNullable(contractSignatureData.get())
                .ifPresent(contractSignatureData -> builder.setContractSignatureData(contractSignatureData.toProto(serializeForHash)));
        return builder;
    }

    public static BisqEasyTradeParty protoToBisqEasyTradeParty(bisq.trade.protobuf.TradeParty proto) {
        return switch (proto.getMessageCase()) {
            case BISQEASYTRADEPARTY -> BisqEasyTradeParty.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
            default -> throw new UnresolvableProtobufMessageException(proto);
        };
    }

    public static BisqMuSigTradeParty protoToBisqMuSigTradeParty(bisq.trade.protobuf.TradeParty proto) {
        return switch (proto.getMessageCase()) {
            case BISQMUSIGTRADEPARTY -> BisqMuSigTradeParty.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
            default -> throw new UnresolvableProtobufMessageException(proto);
        };
    }

    public static SubmarineTradeParty protoToSubmarineTradeParty(bisq.trade.protobuf.TradeParty proto) {
        return switch (proto.getMessageCase()) {
            case SUBMARINETRADEPARTY -> SubmarineTradeParty.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
            default -> throw new UnresolvableProtobufMessageException(proto);
        };
    }
}