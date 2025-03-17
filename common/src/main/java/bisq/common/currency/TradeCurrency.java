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

import bisq.common.annotation.ExcludeForHash;
import bisq.common.proto.PersistableProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public abstract class TradeCurrency implements Comparable<TradeCurrency>, PersistableProto {
    public final static int MAX_NAME_LENGTH = 100;

    protected final String code;
    @ExcludeForHash
    @EqualsAndHashCode.Exclude
    protected final String name;

    public static boolean isFiat(String code) {
        return FiatCurrencyRepository.getCurrencyByCodeMap().containsKey(code);
    }

    public static boolean isBtc(String code) {
        return "BTC".equals(code);
    }

    /**
     * We only can check if the currency is not fiat and if the code matches the format, but we do not maintain a list
     * of cryptocurrencies to be flexible with any newly added one.
     */
    public static boolean isMaybeCrypto(String code) {
        return !isFiat(code) && code.length() >= 3;
    }

    public TradeCurrency(String code, String name) {
        this.code = code;
        this.name = name;

        NetworkDataValidation.validateCode(code);
        NetworkDataValidation.validateText(name, MAX_NAME_LENGTH);
    }

    public bisq.common.protobuf.TradeCurrency.Builder getTradeCurrencyBuilder() {
        return bisq.common.protobuf.TradeCurrency.newBuilder()
                .setCode(code)
                .setName(name);
    }

    public static TradeCurrency fromProto(bisq.common.protobuf.TradeCurrency proto) {
        return switch (proto.getMessageCase()) {
            case CRYPTOCURRENCY -> CryptoCurrency.fromProto(proto);
            case FIATCURRENCY -> FiatCurrency.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    public abstract String getDisplayName();

    public String getDisplayNameAndCode() {
        return getDisplayName() + " (" + code + ")";
    }

    public String getCodeAndDisplayName() {
        return code + " (" + getDisplayName() + ")";
    }

    @Override
    public int compareTo(TradeCurrency other) {
        return this.getDisplayName().compareTo(other.getDisplayName());
    }

    public abstract boolean isFiat();
}
