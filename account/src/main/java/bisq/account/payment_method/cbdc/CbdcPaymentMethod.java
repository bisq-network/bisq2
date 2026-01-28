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

package bisq.account.payment_method.cbdc;

import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.asset.Asset;
import bisq.common.asset.Cbdc;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class CbdcPaymentMethod extends PaymentMethod<CbdcPaymentRail> implements DigitalAssetPaymentMethod {
    public static CbdcPaymentMethod fromPaymentRail(CbdcPaymentRail paymentRail) {
        return new CbdcPaymentMethod(paymentRail);
    }

    public static CbdcPaymentMethod fromCustomName(String customName) {
        return null;
    }

    private CbdcPaymentMethod(CbdcPaymentRail paymentRail) {
        super(paymentRail);

        verify();
    }

    @Override
    public bisq.account.protobuf.PaymentMethod.Builder getBuilder(boolean serializeForHash) {
        return getPaymentMethodBuilder(serializeForHash).setCbdcPaymentMethod(bisq.account.protobuf.CbdcPaymentMethod.newBuilder());
    }

    @Override
    public bisq.account.protobuf.PaymentMethod toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static CbdcPaymentMethod fromProto(bisq.account.protobuf.PaymentMethod proto) {
        return Optional.ofNullable(
                        CbdcPaymentMethodUtil.getPaymentMethod(proto.getPaymentRailName()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown payment method: " + proto.getPaymentRailName()));
    }

    @Override
    protected CbdcPaymentRail getCustomPaymentRail() {
        return null;
    }

    @Override
    public List<Asset> getSupportedCurrencies() {
        return Collections.singletonList(paymentRail.getCbdc());
    }

    @Override
    public String getId() {
        return getCbdc().getCode();
    }

    public String getCode() {
        return getCbdc().getCode();
    }

    public String getName() {
        return getCbdc().getName();
    }

    public String getPegCurrencyCode() {
        return getCbdc().getPegCurrencyCode();
    }

    public String getCountryCode() {
        return getCbdc().getCountryCode();
    }

    private Cbdc getCbdc() {
        return paymentRail.getCbdc();
    }
}
