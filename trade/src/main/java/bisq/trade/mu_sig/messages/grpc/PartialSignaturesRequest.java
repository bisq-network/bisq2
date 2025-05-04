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

package bisq.trade.mu_sig.messages.grpc;

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
public class PartialSignaturesRequest implements Proto {
    private final String tradeId;
    private final NonceSharesMessage peersNonceShares;
    private final List<ReceiverAddressAndAmount> receivers;

    public PartialSignaturesRequest(String tradeId,
                                    NonceSharesMessage peersNonceShares,
                                    List<ReceiverAddressAndAmount> receivers) {
        this.tradeId = tradeId;
        this.peersNonceShares = peersNonceShares;
        this.receivers = receivers;
    }

    @Override
    public bisq.trade.mu_sig.grpc.PartialSignaturesRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.mu_sig.grpc.PartialSignaturesRequest.newBuilder()
                .setTradeId(tradeId)
                .setPeersNonceShares(peersNonceShares.toProto(serializeForHash))
                .addAllReceivers(receivers.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.trade.mu_sig.grpc.PartialSignaturesRequest toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static PartialSignaturesRequest fromProto(bisq.trade.mu_sig.grpc.PartialSignaturesRequest proto) {
        return new PartialSignaturesRequest(proto.getTradeId(),
                NonceSharesMessage.fromProto(proto.getPeersNonceShares()),
                proto.getReceiversList().stream()
                        .map(ReceiverAddressAndAmount::fromProto)
                        .collect(Collectors.toList()));
    }
}