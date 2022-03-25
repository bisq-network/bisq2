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
    // http://boschista.deviantart.com/journal/Cool-ASCII-Symbols-214218618
    private final static String PREFIX = "★ ";

    @Getter
    private final Currency currency;

    public FiatCurrency(String code) {
        this(Currency.getInstance(code), LocaleRepository.getDefaultLocale());
    }

    public FiatCurrency(String code, Locale locale) {
        this(Currency.getInstance(code), locale);
    }

    public FiatCurrency(Currency currency, Locale locale) {
        super(currency.getCurrencyCode(), currency.getDisplayName(locale));

        this.currency = currency;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protobuffer
    ///////////////////////////////////////////////////////////////////////////////////////////

    private FiatCurrency(String code, String name) {
        super(code, name);
        this.currency = Currency.getInstance(code);
    }

    @Override
    public bisq.common.protobuf.TradeCurrency toProto() {
        return getTradeCurrencyBuilder().setFiatCurrency(bisq.common.protobuf.FiatCurrency.newBuilder()).build();
    }

    public static FiatCurrency fromProto(bisq.common.protobuf.TradeCurrency proto) {
        return new FiatCurrency(proto.getCode(), proto.getName());
    }

    @Override
    public String getDisplayPrefix() {
        return PREFIX;
    }

    public boolean isFiat() {
        return true;
    }
}
