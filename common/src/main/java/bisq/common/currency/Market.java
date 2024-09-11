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

import bisq.common.proto.NetworkProto;
import bisq.common.proto.PersistableProto;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Getter
@EqualsAndHashCode
public final class Market implements NetworkProto, PersistableProto, Comparable<Market> {
    public final static int MAX_NAME_LENGTH = 50;
    private static final String QUOTE_SEPARATOR = "/";

    private final String baseCurrencyCode;
    private final String quoteCurrencyCode;

    // The baseCurrencyName and quoteCurrencyName are using the US locale in case they are Fiat currencies, thus they
    // are immutable with the code (therefor we don't need the ExcludeForHash annotation)
    // For display purposes we use getQuoteCurrencyDisplayName() and getBaseCurrencyDisplayName()
    @EqualsAndHashCode.Exclude
    private final String baseCurrencyName;
    @EqualsAndHashCode.Exclude
    private final String quoteCurrencyName;

    public Market(String baseCurrencyCode,
                  String quoteCurrencyCode,
                  String baseCurrencyName,
                  String quoteCurrencyName) {
        this.baseCurrencyCode = baseCurrencyCode;
        this.quoteCurrencyCode = quoteCurrencyCode;
        this.baseCurrencyName = baseCurrencyName;
        this.quoteCurrencyName = quoteCurrencyName;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateCode(baseCurrencyCode);
        NetworkDataValidation.validateCode(quoteCurrencyCode);
        NetworkDataValidation.validateText(baseCurrencyName, MAX_NAME_LENGTH);
        NetworkDataValidation.validateText(quoteCurrencyName, MAX_NAME_LENGTH);
    }

    @Override
    public bisq.common.protobuf.Market.Builder getBuilder(boolean serializeForHash) {
        return bisq.common.protobuf.Market.newBuilder()
                .setBaseCurrencyCode(baseCurrencyCode)
                .setQuoteCurrencyCode(quoteCurrencyCode)
                .setBaseCurrencyName(baseCurrencyName)
                .setQuoteCurrencyName(quoteCurrencyName);
    }

    @Override
    public bisq.common.protobuf.Market toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static Market fromProto(bisq.common.protobuf.Market proto) {
        return new Market(proto.getBaseCurrencyCode(),
                proto.getQuoteCurrencyCode(),
                proto.getBaseCurrencyName(),
                proto.getQuoteCurrencyName());
    }

    public String getQuoteCurrencyDisplayName() {
        return FiatCurrency.isFiat(quoteCurrencyCode)
                ? FiatCurrencyRepository.getDisplayName(quoteCurrencyCode).orElse(quoteCurrencyName)
                : quoteCurrencyName;
    }

    public String getBaseCurrencyDisplayName() {
        return FiatCurrency.isFiat(baseCurrencyCode)
                ? FiatCurrencyRepository.getDisplayName(baseCurrencyCode).orElse(baseCurrencyName)
                : baseCurrencyName;
    }

    //todo (refactor, low prio) make static utils
    public String getFiatCurrencyName() {
        return isFiat() ? getQuoteCurrencyDisplayName() : getBaseCurrencyDisplayName();
    }

    public boolean isFiat() {
        return FiatCurrency.isFiat(quoteCurrencyCode);
    }

    public String getMarketCodes() {
        return baseCurrencyCode + QUOTE_SEPARATOR + quoteCurrencyCode;
    }
    public static String createBitcoinFiatMarketCodes(String baseCurrencyCode, String quoteCurrencyCode) {
        return baseCurrencyCode + QUOTE_SEPARATOR + quoteCurrencyCode;
    }

    public String getMarketDisplayName() {
        return getBaseCurrencyDisplayName() + QUOTE_SEPARATOR + getQuoteCurrencyDisplayName();
    }

    @Override
    public String toString() {
        return getFiatCurrencyName() + " (" + getMarketCodes() + ")";
    }

    @Override
    public int compareTo(@NotNull Market o) {
        return this.getMarketCodes().compareTo(o.getMarketCodes());
    }
}