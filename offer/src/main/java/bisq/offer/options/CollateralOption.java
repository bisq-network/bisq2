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

package bisq.offer.options;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@ToString
@EqualsAndHashCode
public final class CollateralOption implements OfferOption {
    private final double buyerSecurityDeposit;
    private final double sellerSecurityDeposit;

    public CollateralOption(double buyerSecurityDeposit, double sellerSecurityDeposit) {
        this.buyerSecurityDeposit = buyerSecurityDeposit;
        this.sellerSecurityDeposit = sellerSecurityDeposit;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(Double.isFinite(buyerSecurityDeposit),
                "The buyer security deposit must be finite but was %s", buyerSecurityDeposit);
        checkArgument(Double.isFinite(sellerSecurityDeposit),
                "The seller security deposit must be finite but was %s", sellerSecurityDeposit);
        // Double.compare orders -0.0 below +0.0, so the non-canonical zero is rejected along
        // with negative values; consumers compare deposits with Double.compare semantics.
        checkArgument(Double.compare(buyerSecurityDeposit, 0) >= 0 && buyerSecurityDeposit <= 1,
                "The buyer security deposit must be between 0 and 1 but was %s", buyerSecurityDeposit);
        checkArgument(Double.compare(sellerSecurityDeposit, 0) >= 0 && sellerSecurityDeposit <= 1,
                "The seller security deposit must be between 0 and 1 but was %s", sellerSecurityDeposit);
    }

    public bisq.offer.protobuf.OfferOption.Builder getBuilder(boolean serializeForHash) {
        return getOfferOptionBuilder(serializeForHash)
                .setCollateralOption(bisq.offer.protobuf.CollateralOption.newBuilder()
                        .setBuyerSecurityDeposit(buyerSecurityDeposit)
                        .setSellerSecurityDeposit(sellerSecurityDeposit));
    }

    @Override
    public bisq.offer.protobuf.OfferOption toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static CollateralOption fromProto(bisq.offer.protobuf.CollateralOption proto) {
        return new CollateralOption(proto.getBuyerSecurityDeposit(), proto.getSellerSecurityDeposit());
    }
}