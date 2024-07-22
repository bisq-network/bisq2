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

package bisq.common.currency;


import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class CryptoCurrency extends TradeCurrency {
    public CryptoCurrency(String code, String name) {
        super(code, name);
    }

    @Override
    public bisq.common.protobuf.TradeCurrency.Builder getBuilder(boolean serializeForHash) {
        return getTradeCurrencyBuilder().setCryptoCurrency(bisq.common.protobuf.CryptoCurrency.newBuilder());
    }

    @Override
    public bisq.common.protobuf.TradeCurrency toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static CryptoCurrency fromProto(bisq.common.protobuf.TradeCurrency baseProto) {
        return new CryptoCurrency(baseProto.getCode(), baseProto.getName());
    }

    public boolean isFiat() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return name;
    }
}
