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

package bisq.trade.mu_sig;

import bisq.common.proto.PersistableProto;
import bisq.trade.mu_sig.messages.grpc.CustomCloseTradeResponse;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbt;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PeerCustomPayoutPsbt;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@EqualsAndHashCode
@Getter
public final class MuSigCustomPayoutPartyData implements PersistableProto {
    private final Optional<CustomPayoutPsbt> myCustomPayoutPsbt;
    private final Optional<PeerCustomPayoutPsbt> peersCustomPayoutPsbt;
    private Optional<CustomCloseTradeResponse> myCustomCloseTradeResponse;

    public static MuSigCustomPayoutPartyData forLocalParty(CustomPayoutPsbt myCustomPayoutPsbt) {
        return new MuSigCustomPayoutPartyData(
                Optional.of(myCustomPayoutPsbt),
                Optional.empty(),
                Optional.empty());
    }

    public static MuSigCustomPayoutPartyData forPeerParty(PeerCustomPayoutPsbt peersCustomPayoutPsbt) {
        return new MuSigCustomPayoutPartyData(
                Optional.empty(),
                Optional.of(peersCustomPayoutPsbt),
                Optional.empty());
    }

    private MuSigCustomPayoutPartyData(Optional<CustomPayoutPsbt> myCustomPayoutPsbt,
                                       Optional<PeerCustomPayoutPsbt> peersCustomPayoutPsbt,
                                       Optional<CustomCloseTradeResponse> myCustomCloseTradeResponse) {
        this.myCustomPayoutPsbt = myCustomPayoutPsbt;
        this.peersCustomPayoutPsbt = peersCustomPayoutPsbt;
        this.myCustomCloseTradeResponse = myCustomCloseTradeResponse;

        verify();
    }

    private void verify() {
        boolean hasLocalData = myCustomPayoutPsbt.isPresent()
                || myCustomCloseTradeResponse.isPresent();
        boolean hasPeerData = peersCustomPayoutPsbt.isPresent();

        checkArgument(hasLocalData != hasPeerData,
                "Custom payout party data must contain either local data or peer data");
        if (hasLocalData) {
            checkArgument(myCustomPayoutPsbt.isPresent(),
                    "Local custom payout data requires a local PSBT");
        }
    }

    @Override
    public bisq.trade.protobuf.MuSigCustomPayoutPartyData.Builder getBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigCustomPayoutPartyData.Builder builder =
                bisq.trade.protobuf.MuSigCustomPayoutPartyData.newBuilder();
        myCustomPayoutPsbt.ifPresent(value -> builder.setMyCustomPayoutPsbt(value.toProto(serializeForHash)));
        peersCustomPayoutPsbt.ifPresent(value -> builder.setPeersCustomPayoutPsbt(value.toProto(serializeForHash)));
        myCustomCloseTradeResponse.ifPresent(value ->
                builder.setMyCustomCloseTradeResponse(value.toProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.trade.protobuf.MuSigCustomPayoutPartyData toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigCustomPayoutPartyData fromProto(bisq.trade.protobuf.MuSigCustomPayoutPartyData proto) {
        return new MuSigCustomPayoutPartyData(
                proto.hasMyCustomPayoutPsbt()
                        ? Optional.of(CustomPayoutPsbt.fromProto(proto.getMyCustomPayoutPsbt()))
                        : Optional.empty(),
                proto.hasPeersCustomPayoutPsbt()
                        ? Optional.of(PeerCustomPayoutPsbt.fromProto(proto.getPeersCustomPayoutPsbt()))
                        : Optional.empty(),
                proto.hasMyCustomCloseTradeResponse()
                        ? Optional.of(CustomCloseTradeResponse.fromProto(proto.getMyCustomCloseTradeResponse()))
                        : Optional.empty());
    }

    boolean isLocalPartyData() {
        return myCustomPayoutPsbt.isPresent();
    }

    boolean setMyCustomCloseTradeResponse(CustomCloseTradeResponse myCustomCloseTradeResponse) {
        checkArgument(isLocalPartyData(), "A custom close response can only be stored with local party data");
        if (this.myCustomCloseTradeResponse.isPresent()) {
            return false;
        }
        this.myCustomCloseTradeResponse = Optional.of(myCustomCloseTradeResponse);
        return true;
    }
}
