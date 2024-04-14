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

package bisq.offer.price.spec;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * The current market price is used.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class MarketPriceSpec implements PriceSpec {

    public MarketPriceSpec() {
        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.offer.protobuf.PriceSpec.Builder getBuilder(boolean serializeForHash) {
        return getPriceSpecBuilder(serializeForHash).setMarketPrice(bisq.offer.protobuf.MarketPrice.newBuilder());
    }

    @Override
    public bisq.offer.protobuf.PriceSpec toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static MarketPriceSpec fromProto(bisq.offer.protobuf.MarketPrice proto) {
        return new MarketPriceSpec();
    }
}