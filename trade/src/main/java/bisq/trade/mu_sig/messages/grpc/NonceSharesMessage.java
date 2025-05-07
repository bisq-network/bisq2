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
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class NonceSharesMessage implements Proto {
    private final String warningTxFeeBumpAddress;
    private final String redirectTxFeeBumpAddress;
    private final byte[] halfDepositPsbt;
    private final byte[] swapTxInputNonceShare;
    private final byte[] buyersWarningTxBuyerInputNonceShare;
    private final byte[] buyersWarningTxSellerInputNonceShare;
    private final byte[] sellersWarningTxBuyerInputNonceShare;
    private final byte[] sellersWarningTxSellerInputNonceShare;
    private final byte[] buyersRedirectTxInputNonceShare;
    private final byte[] sellersRedirectTxInputNonceShare;

    public NonceSharesMessage(String warningTxFeeBumpAddress,
            String redirectTxFeeBumpAddress,
            byte[] halfDepositPsbt,
            byte[] swapTxInputNonceShare,
            byte[] buyersWarningTxBuyerInputNonceShare,
            byte[] buyersWarningTxSellerInputNonceShare,
            byte[] sellersWarningTxBuyerInputNonceShare,
            byte[] sellersWarningTxSellerInputNonceShare,
            byte[] buyersRedirectTxInputNonceShare,
            byte[] sellersRedirectTxInputNonceShare) {
        this.warningTxFeeBumpAddress = warningTxFeeBumpAddress;
        this.redirectTxFeeBumpAddress = redirectTxFeeBumpAddress;
        this.halfDepositPsbt = halfDepositPsbt;
        this.swapTxInputNonceShare = swapTxInputNonceShare;
        this.buyersWarningTxBuyerInputNonceShare = buyersWarningTxBuyerInputNonceShare;
        this.buyersWarningTxSellerInputNonceShare = buyersWarningTxSellerInputNonceShare;
        this.sellersWarningTxBuyerInputNonceShare = sellersWarningTxBuyerInputNonceShare;
        this.sellersWarningTxSellerInputNonceShare = sellersWarningTxSellerInputNonceShare;
        this.buyersRedirectTxInputNonceShare = buyersRedirectTxInputNonceShare;
        this.sellersRedirectTxInputNonceShare = sellersRedirectTxInputNonceShare;
    }

    @Override
    public bisq.trade.protobuf.NonceSharesMessage.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.NonceSharesMessage.newBuilder()
                .setWarningTxFeeBumpAddress(warningTxFeeBumpAddress)
                .setRedirectTxFeeBumpAddress(redirectTxFeeBumpAddress)
                .setHalfDepositPsbt(ByteString.copyFrom(halfDepositPsbt))
                .setSwapTxInputNonceShare(ByteString.copyFrom(swapTxInputNonceShare))
                .setBuyersWarningTxBuyerInputNonceShare(ByteString.copyFrom(buyersWarningTxBuyerInputNonceShare))
                .setBuyersWarningTxSellerInputNonceShare(ByteString.copyFrom(buyersWarningTxSellerInputNonceShare))
                .setSellersWarningTxBuyerInputNonceShare(ByteString.copyFrom(sellersWarningTxBuyerInputNonceShare))
                .setSellersWarningTxSellerInputNonceShare(ByteString.copyFrom(sellersWarningTxSellerInputNonceShare))
                .setBuyersRedirectTxInputNonceShare(ByteString.copyFrom(buyersRedirectTxInputNonceShare))
                .setSellersRedirectTxInputNonceShare(ByteString.copyFrom(sellersRedirectTxInputNonceShare));
    }

    @Override
    public bisq.trade.protobuf.NonceSharesMessage toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static NonceSharesMessage fromProto(bisq.trade.protobuf.NonceSharesMessage proto) {
        return new NonceSharesMessage(proto.getWarningTxFeeBumpAddress(),
                proto.getRedirectTxFeeBumpAddress(),
                proto.getHalfDepositPsbt().toByteArray(),
                proto.getSwapTxInputNonceShare().toByteArray(),
                proto.getBuyersWarningTxBuyerInputNonceShare().toByteArray(),
                proto.getBuyersWarningTxSellerInputNonceShare().toByteArray(),
                proto.getSellersWarningTxBuyerInputNonceShare().toByteArray(),
                proto.getSellersWarningTxSellerInputNonceShare().toByteArray(),
                proto.getBuyersRedirectTxInputNonceShare().toByteArray(),
                proto.getSellersRedirectTxInputNonceShare().toByteArray());
    }
}
