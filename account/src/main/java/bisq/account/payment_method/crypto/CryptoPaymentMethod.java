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

package bisq.account.payment_method.crypto;

import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.asset.Asset;
import bisq.common.asset.CryptoAsset;
import bisq.common.asset.CryptoAssetRepository;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class CryptoPaymentMethod extends PaymentMethod<CryptoPaymentRail> implements DigitalAssetPaymentMethod {
    private final String code;
    private final transient boolean isFeatured;
    private final transient CryptoAsset cryptoAsset;

    public static CryptoPaymentMethod fromCustomName(String customName, String code) {
        return new CryptoPaymentMethod(customName, code);
    }

    public CryptoPaymentMethod(String code) {
        super(CryptoPaymentMethodUtil.getCryptoPaymentRail(code));
        this.code = code;
        Optional<CryptoAsset> optionalCryptoAsset = CryptoAssetRepository.find(code);
        isFeatured = optionalCryptoAsset.isPresent();
        cryptoAsset = optionalCryptoAsset.orElseGet(() -> new CryptoAsset(code));

        verify();
    }

    public CryptoPaymentMethod(CryptoPaymentRail cryptoPaymentRail, String code) {
        super(cryptoPaymentRail);
        this.code = code;
        Optional<CryptoAsset> optionalCryptoAsset = CryptoAssetRepository.find(code);
        isFeatured = optionalCryptoAsset.isPresent();
        cryptoAsset = optionalCryptoAsset.orElseGet(() -> new CryptoAsset(code));

        verify();
    }

    private CryptoPaymentMethod(String customPaymentRailName, String code) {
        super(customPaymentRailName);
        this.code = code;
        Optional<CryptoAsset> optionalCryptoAsset = CryptoAssetRepository.find(code);
        isFeatured = optionalCryptoAsset.isPresent();
        cryptoAsset = optionalCryptoAsset.orElseGet(() -> new CryptoAsset(code));

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateCode(code);
    }

    @Override
    public bisq.account.protobuf.PaymentMethod.Builder getBuilder(boolean serializeForHash) {
        return getPaymentMethodBuilder(serializeForHash).setCryptoPaymentMethod(
                bisq.account.protobuf.CryptoPaymentMethod.newBuilder()
                        .setCode(code));
    }

    @Override
    public bisq.account.protobuf.PaymentMethod toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static CryptoPaymentMethod fromProto(bisq.account.protobuf.PaymentMethod proto) {
        return new CryptoPaymentMethod(proto.getPaymentRailName(), proto.getCryptoPaymentMethod().getCode());
    }

    @Override
    public String getDisplayString() {
        return cryptoAsset.getDisplayNameAndCode();
    }

    @Override
    public String getShortDisplayString() {
        return code;
    }

    @Override
    public List<Asset> getSupportedCurrencies() {
        return Collections.singletonList(cryptoAsset);
    }

    @Override
    protected CryptoPaymentRail getCustomPaymentRail() {
        return CryptoPaymentRail.CUSTOM;
    }

    @Override
    public String getId() {
        return getCode();
    }

    public String getName() {
        return cryptoAsset.getName();
    }
}
