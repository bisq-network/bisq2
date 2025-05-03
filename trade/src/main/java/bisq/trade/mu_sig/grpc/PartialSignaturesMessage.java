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

import java.util.Objects;
import java.util.Optional;
@Getter
public class PartialSignaturesMessage {

    private final byte[] peersWarningTxBuyerInputPartialSignature;
    private final byte[] peersWarningTxSellerInputPartialSignature;
    private final byte[] peersRedirectTxInputPartialSignature;
    private final Optional<byte[]> swapTxInputPartialSignature;

    public PartialSignaturesMessage(byte[] peersWarningTxBuyerInputPartialSignature,
                                    byte[] peersWarningTxSellerInputPartialSignature,
                                    byte[] peersRedirectTxInputPartialSignature,
                                    Optional<byte[]> swapTxInputPartialSignature) {
        this.peersWarningTxBuyerInputPartialSignature = Objects.requireNonNull(peersWarningTxBuyerInputPartialSignature);
        this.peersWarningTxSellerInputPartialSignature = Objects.requireNonNull(peersWarningTxSellerInputPartialSignature);
        this.peersRedirectTxInputPartialSignature = Objects.requireNonNull(peersRedirectTxInputPartialSignature);
        this.swapTxInputPartialSignature = Objects.requireNonNull(swapTxInputPartialSignature);
    }
}
