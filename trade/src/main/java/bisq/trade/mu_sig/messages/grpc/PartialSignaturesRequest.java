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
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
public final class PartialSignaturesRequest implements Proto {
    private final String tradeId;
    private final Optional<NonceSharesMessage> peersNonceShares;
    private final List<ReceiverAddressAndAmount> redirectionReceivers;
    private final boolean buyerReadyToRelease;

    public PartialSignaturesRequest(String tradeId,
                                    Optional<NonceSharesMessage> peersNonceShares,
                                    List<ReceiverAddressAndAmount> redirectionReceivers,
                                    boolean buyerReadyToRelease) {
        this.tradeId = tradeId;
        this.peersNonceShares = peersNonceShares;
        this.redirectionReceivers = redirectionReceivers;
        this.buyerReadyToRelease = buyerReadyToRelease;
    }

    @Override
    public bisq.trade.protobuf.PartialSignaturesRequest.Builder getBuilder(boolean serializeForHash) {
        var builder = bisq.trade.protobuf.PartialSignaturesRequest.newBuilder()
                .setTradeId(tradeId)
                .addAllRedirectionReceivers(redirectionReceivers.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()))
                .setBuyerReadyToRelease(buyerReadyToRelease);
        peersNonceShares.ifPresent(e -> builder.setPeersNonceShares(e.toProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.trade.protobuf.PartialSignaturesRequest toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static PartialSignaturesRequest fromProto(bisq.trade.protobuf.PartialSignaturesRequest proto) {
        return new PartialSignaturesRequest(proto.getTradeId(),
                proto.hasPeersNonceShares()
                        ? Optional.of(NonceSharesMessage.fromProto(proto.getPeersNonceShares()))
                        : Optional.empty(),
                proto.getRedirectionReceiversList().stream()
                        .map(ReceiverAddressAndAmount::fromProto)
                        .collect(Collectors.toList()),
                proto.getBuyerReadyToRelease());
    }
}
