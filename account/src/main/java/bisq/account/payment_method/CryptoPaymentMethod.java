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

package bisq.account.payment_method;

import bisq.common.asset.CryptoCurrencyRepository;
import bisq.common.asset.Asset;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class CryptoPaymentMethod extends PaymentMethod<CryptoPaymentRail> {
    private final String currencyCode;
    private final String currencyName;

    public static CryptoPaymentMethod fromCustomName(String customName, String currencyCode) {
        return new CryptoPaymentMethod(customName, currencyCode);
    }

    public CryptoPaymentMethod(CryptoPaymentRail cryptoPaymentRail, String currencyCode) {
        super(cryptoPaymentRail);
        this.currencyCode = currencyCode;
        this.currencyName = CryptoCurrencyRepository.findName(currencyCode).orElse(currencyCode);

        verify();
    }

    private CryptoPaymentMethod(String name, String currencyCode) {
        super(name);
        this.currencyCode = currencyCode;
        this.currencyName = CryptoCurrencyRepository.findName(currencyCode).orElse(currencyCode);

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateCode(currencyCode);
    }

    public String getCurrencyNameAndCode() {
        return currencyName + " (" + currencyCode + ")";
    }

    @Override
    public String getDisplayString() {
        return currencyName;
    }

    @Override
    public String getShortDisplayString() {
        return currencyCode;
    }

    @Override
    public bisq.account.protobuf.PaymentMethod.Builder getBuilder(boolean serializeForHash) {
        return getPaymentMethodBuilder(serializeForHash).setCryptoPaymentMethod(
                bisq.account.protobuf.CryptoPaymentMethod.newBuilder()
                        .setCurrencyCode(currencyCode));
    }

    @Override
    public bisq.account.protobuf.PaymentMethod toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static CryptoPaymentMethod fromProto(bisq.account.protobuf.PaymentMethod proto) {
        return CryptoPaymentMethodUtil.getPaymentMethod(proto.getPaymentRailName(), proto.getCryptoPaymentMethod().getCurrencyCode());
    }

    @Override
    protected CryptoPaymentRail getCustomPaymentRail() {
        return CryptoPaymentRail.CUSTOM;
    }

    @Override
    public List<Asset> getSupportedCurrencies() {
        return CryptoCurrencyRepository.find(currencyCode)
                .map(e -> List.of((Asset) e))
                .orElse(new ArrayList<>());
    }
}
