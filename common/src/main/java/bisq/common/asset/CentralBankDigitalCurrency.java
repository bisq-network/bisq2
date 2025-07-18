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


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class CentralBankDigitalCurrency extends DigitalAsset {
    private final String pegCurrencyCode;
    private final String countryCode;

    public CentralBankDigitalCurrency(String code,
                                      String name,
                                      String pegCurrencyCode,
                                      String countryCode) {
        super(code, name);
        this.pegCurrencyCode = pegCurrencyCode;
        this.countryCode = countryCode;
    }


    @Override
    public bisq.common.protobuf.Asset.Builder getBuilder(boolean serializeForHash) {
        return getAssetBuilder().setDigitalAsset(bisq.common.protobuf.DigitalAsset.newBuilder()
                .setCentralBankDigitalCurrency(
                        bisq.common.protobuf.CentralBankDigitalCurrency.newBuilder()
                                .setPegCurrencyCode(pegCurrencyCode)
                                .setCountryCode(countryCode)));
    }

    @Override
    public bisq.common.protobuf.Asset toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static CentralBankDigitalCurrency fromProto(bisq.common.protobuf.Asset baseProto) {
        bisq.common.protobuf.CentralBankDigitalCurrency proto = baseProto.getDigitalAsset().getCentralBankDigitalCurrency();
        return new CentralBankDigitalCurrency(baseProto.getCode(), baseProto.getName(),
                proto.getPegCurrencyCode(),
                proto.getCountryCode());
    }

    @Override
    public boolean isCustom() {
        return CentralBankDigitalCurrencyRepository.find(code).isEmpty();
    }

    @Override
    public String getDisplayName() {
        return name + " (" + code + ")";
    }

    public String getShortDisplayName() {
        return code;
    }
}
