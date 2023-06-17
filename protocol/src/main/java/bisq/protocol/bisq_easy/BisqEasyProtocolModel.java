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

package bisq.protocol.bisq_easy;

import bisq.common.observable.Observable;
import bisq.common.proto.Proto;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.protocol.bisq_easy.states.BisqEasyState;
import bisq.protocol.fsm.State;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString
@Slf4j
@EqualsAndHashCode
@Getter
public class BisqEasyProtocolModel implements Proto {
    public static String createProtocolId(String offerId, String takerNodeId) {
        return offerId + "." + takerNodeId;
    }

    private final BisqEasyContract bisqEasyContract;

    private final ProtocolParty taker;
    private final ProtocolParty maker;
    private final Observable<State> fsmState = new Observable<>(BisqEasyState.INIT);

    public BisqEasyProtocolModel(BisqEasyContract bisqEasyContract, NetworkId takerNetworkId) {
        this.bisqEasyContract = bisqEasyContract;
        taker = new ProtocolParty(takerNetworkId);
        maker = new ProtocolParty(bisqEasyContract.getOffer().getMakerNetworkId());
    }

    @Override
    public bisq.protocol.protobuf.BisqEasyProtocolModel toProto() {
        return bisq.protocol.protobuf.BisqEasyProtocolModel.newBuilder()
                .build();
    }

    public static BisqEasyProtocolModel fromProto(bisq.protocol.protobuf.BisqEasyProtocolModel proto) {
        return new BisqEasyProtocolModel(null, null);
    }

    public String getProtocolId() {
        return createProtocolId(getOffer().getId(), taker.getNetworkId().getNodeId());
    }

    public BisqEasyOffer getOffer() {
        return bisqEasyContract.getOffer();
    }

    public void sendPaymentAccount() {

    }
}