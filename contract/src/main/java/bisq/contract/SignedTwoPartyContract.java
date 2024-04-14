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

package bisq.contract;

import bisq.common.proto.NetworkProto;
import bisq.offer.Offer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@EqualsAndHashCode
public class SignedTwoPartyContract<T extends Offer<?, ?>> implements NetworkProto {
    private final TwoPartyContract<T> contract;
    private final ContractSignatureData makerSignatureData;
    private final ContractSignatureData takerSignatureData;

    public SignedTwoPartyContract(TwoPartyContract<T> contract, ContractSignatureData makerSignatureData, ContractSignatureData takerSignatureData) {
        this.contract = contract;
        this.makerSignatureData = makerSignatureData;
        this.takerSignatureData = takerSignatureData;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.contract.protobuf.SignedTwoPartyContract.Builder getBuilder(boolean serializeForHash) {
        return bisq.contract.protobuf.SignedTwoPartyContract.newBuilder()
                .setContract(contract.toProto(serializeForHash))
                .setMakerSignatureData(makerSignatureData.toProto(serializeForHash))
                .setTakerSignatureData(takerSignatureData.toProto(serializeForHash));
    }

    @Override
    public bisq.contract.protobuf.SignedTwoPartyContract toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static SignedTwoPartyContract<?> fromProto(bisq.contract.protobuf.SignedTwoPartyContract proto) {
        return new SignedTwoPartyContract<>(TwoPartyContract.fromProto(proto.getContract()),
                ContractSignatureData.fromProto(proto.getMakerSignatureData()),
                ContractSignatureData.fromProto(proto.getTakerSignatureData()));
    }
}