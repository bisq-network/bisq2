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

import bisq.common.proto.Proto;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString
@Slf4j
@EqualsAndHashCode
@Getter
public class BisqEasyProtocolModel implements Proto {
    private final BisqEasyContract bisqEasyContract;
    private final ContractSignatureData contractSignatureData;
    private final ProtocolParty taker;
    private final ProtocolParty maker;

    public BisqEasyProtocolModel(Identity takerIdentity, BisqEasyContract bisqEasyContract, ContractSignatureData contractSignatureData) {
        this.taker = new ProtocolParty(takerIdentity.getNetworkId());
        this.bisqEasyContract = bisqEasyContract;
        this.contractSignatureData = contractSignatureData;

        this.maker = new ProtocolParty(getOffer().getMakerNetworkId());
    }

    //todo
    public BisqEasyProtocolModel() {
        taker = null;
        bisqEasyContract = null;
        contractSignatureData = null;
        maker = null;
    }

    @Override
    public bisq.protocol.protobuf.BisqEasyProtocolModel toProto() {
        return bisq.protocol.protobuf.BisqEasyProtocolModel.newBuilder()
                .build();
    }

    public static BisqEasyProtocolModel fromProto(bisq.protocol.protobuf.BisqEasyProtocolModel proto) {
        return new BisqEasyProtocolModel();
    }

    public String getProtocolId() {
        return getOffer().getId() + "." + taker.getNetworkId().getNodeId();
    }

    public BisqEasyOffer getOffer() {
        return bisqEasyContract.getOffer();
    }

}