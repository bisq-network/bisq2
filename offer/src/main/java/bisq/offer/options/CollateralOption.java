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

@Getter
@ToString
@EqualsAndHashCode
public final class CollateralOption implements OfferOption {
    private final long buyerSecurityDeposit;
    private final long sellerSecurityDeposit;

    public CollateralOption(long buyerSecurityDeposit, long sellerSecurityDeposit) {
        this.buyerSecurityDeposit = buyerSecurityDeposit;
        this.sellerSecurityDeposit = sellerSecurityDeposit;

        verify();
    }

    @Override
    public void verify() {
    }

    public bisq.offer.protobuf.OfferOption.Builder getBuilder(boolean serializeForHash) {
        return getOfferOptionBuilder(serializeForHash)
                .setCollateralOption(bisq.offer.protobuf.CollateralOption.newBuilder()
                        .setBuyerSecurityDeposit(buyerSecurityDeposit)
                        .setSellerSecurityDeposit(sellerSecurityDeposit));
    }

    @Override
    public bisq.offer.protobuf.OfferOption toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static CollateralOption fromProto(bisq.offer.protobuf.CollateralOption proto) {
        return new CollateralOption(proto.getBuyerSecurityDeposit(), proto.getSellerSecurityDeposit());
    }
}