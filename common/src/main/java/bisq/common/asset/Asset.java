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

package bisq.common.asset;

import bisq.common.annotation.ExcludeForHash;
import bisq.common.proto.PersistableProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@EqualsAndHashCode
@ToString
@Getter
public abstract class Asset implements Comparable<Asset>, PersistableProto {
    public final static int MAX_NAME_LENGTH = 100;

    protected final String code;
    @ExcludeForHash
    @EqualsAndHashCode.Exclude
    protected final String name;

    public Asset(String code, String name) {
        checkNotNull(code, "code must not be null");
        checkNotNull(name, "name must not be null");

        this.code = code;
        this.name = name;

        NetworkDataValidation.validateCode(code);
        NetworkDataValidation.validateText(name, MAX_NAME_LENGTH);
    }

    @Override
    public bisq.common.protobuf.Asset toProto(boolean serializeForHash) {
        return switch (this) {
            case FiatCurrency fiatCurrency -> fiatCurrency.toProto(serializeForHash);
            case CryptoAsset cryptoAsset -> cryptoAsset.toProto(serializeForHash);
            case StableCoin stableCoinCurrency -> stableCoinCurrency.toProto(serializeForHash);
            default -> throw new UnsupportedOperationException("Unsupported tradeCurrency at toProto {}" + name);
        };
    }

    public bisq.common.protobuf.Asset.Builder getAssetBuilder() {
        return bisq.common.protobuf.Asset.newBuilder()
                .setCode(code)
                .setName(name);
    }

    public static Asset fromProto(bisq.common.protobuf.Asset proto) {
        return switch (proto.getMessageCase()) {
            case FIATCURRENCY -> FiatCurrency.fromProto(proto);
            case DIGITALASSET -> DigitalAsset.fromProto(proto);
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
    public int compareTo(Asset other) {
        return this.getDisplayName().compareTo(other.getDisplayName());
    }

    //todo

    public static boolean isFiat(String code) {
        return FiatCurrencyRepository.getCurrencyByCodeMap().containsKey(code);
    }

    public static boolean isBtc(String code) {
        return "BTC".equals(code);
    }

    public static boolean isBsq(String code) {
        return "BSQ".equals(code);
    }

    // Should also exclude stable coins
    public static boolean isAltcoin(String code) {
        return !isFiat(code) && !isBtc(code) && !isBsq(code);
    }

    /**
     * We only can check if the currency is not fiat and if the code matches the format, but we do not maintain a list
     * of cryptocurrencies to be flexible with any newly added one.
     */
    public static boolean isMaybeCrypto(String code) {
        return !isFiat(code) && code.length() >= 3;
    }
}
