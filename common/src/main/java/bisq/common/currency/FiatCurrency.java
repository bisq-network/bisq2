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


import bisq.common.locale.LocaleRepository;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Currency;
import java.util.Locale;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class FiatCurrency extends TradeCurrency {
    @Getter
    // transient fields are excluded by default for EqualsAndHashCode
    private transient final Currency currency;
    private transient String displayName;

    public FiatCurrency(String code) {
        this(Currency.getInstance(code));
    }

    public FiatCurrency(Currency currency) {
        super(currency.getCurrencyCode(), currency.getDisplayName(Locale.US));
        this.currency = currency;
    }

    private FiatCurrency(String code, String name) {
        super(code, name);
        this.currency = Currency.getInstance(code);
    }

    @Override
    public bisq.common.protobuf.TradeCurrency.Builder getBuilder(boolean serializeForHash) {
        return getTradeCurrencyBuilder().setFiatCurrency(bisq.common.protobuf.FiatCurrency.newBuilder());
    }

    @Override
    public bisq.common.protobuf.TradeCurrency toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static FiatCurrency fromProto(bisq.common.protobuf.TradeCurrency baseProto) {
        return new FiatCurrency(baseProto.getCode(), baseProto.getName());
    }

    // The name field is the displayName using the US locale. For display purpose we use the name based on the user's locale.
    @Override
    public String getDisplayName() {
        if (displayName == null) {
            Locale defaultLocale = LocaleRepository.getDefaultLocale();
            displayName = currency.getDisplayName(defaultLocale);
        }
        return displayName;
    }

    public boolean isFiat() {
        return true;
    }
}
