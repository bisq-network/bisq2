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

package bisq.trade.mu_sig.grpc;

import lombok.Getter;

@Getter
public class NonceSharesMessage {
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
}
