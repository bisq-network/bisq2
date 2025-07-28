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

package bisq.burningman;

import bisq.common.proto.NetworkProto;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Even the Burningman is not yet directly related to the Bisq user, it might become related in future in case we allow
 * users to become a burningman directly from Bisq 2 or use it for reputation and security aspects.
 * For that reason we leave that domain inside user instead of trade, where it is currently used, but it would feel
 * the right place as burningman is not directly part of the trade but just an auxiliary aspect of trade.
 */
@Slf4j
@Getter
@ToString
public final class BurningmanData implements NetworkProto {
    private final String receiverAddress;
    private final double cappedBurnAmountShare;

    public BurningmanData(String receiverAddress,
                          double cappedBurnAmountShare
    ) {
        this.receiverAddress = receiverAddress;
        this.cappedBurnAmountShare = cappedBurnAmountShare;

        verify();
    }

    @Override
    public void verify() {
     /*   checkArgument(amount > 0);
        NetworkDataValidation.validateDate(blockTime);
        NetworkDataValidation.validateHash(hash);
        if (version > 0) {
            NetworkDataValidation.validateBtcTxId(txId);
            checkArgument(blockHeight > 0);
        }*/
    }

    @Override
    public bisq.burningman.protobuf.BurningmanData.Builder getBuilder(boolean serializeForHash) {
        return bisq.burningman.protobuf.BurningmanData.newBuilder()
                .setReceiverAddress(receiverAddress)
                .setCappedBurnAmountShare(cappedBurnAmountShare);
    }

    @Override
    public bisq.burningman.protobuf.BurningmanData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BurningmanData fromProto(bisq.burningman.protobuf.BurningmanData proto) {
        return new BurningmanData(proto.getReceiverAddress(), proto.getCappedBurnAmountShare());
    }
}