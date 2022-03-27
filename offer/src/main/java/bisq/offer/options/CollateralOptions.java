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

public record CollateralOptions(long buyerSecurityDeposit, long sellerSecurityDeposit) implements ListingOption {
    public bisq.offer.protobuf.ListingOption toProto() {
        return getListingOptionBuilder().setCollateralOptions(bisq.offer.protobuf.CollateralOptions.newBuilder()
                        .setBuyerSecurityDeposit(buyerSecurityDeposit)
                        .setSellerSecurityDeposit(sellerSecurityDeposit))
                .build();
    }

    public static CollateralOptions fromProto(bisq.offer.protobuf.CollateralOptions proto) {
        return new CollateralOptions(proto.getBuyerSecurityDeposit(), proto.getSellerSecurityDeposit());
    }
}