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

import bisq.common.currency.TradeCurrency;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class FiatPaymentMethod extends PaymentMethod<FiatPaymentRail> {
    public static FiatPaymentMethod fromPaymentRail(FiatPaymentRail fiatPaymentRail) {
        return new FiatPaymentMethod(fiatPaymentRail);
    }

    public static FiatPaymentMethod fromCustomName(String customName) {
        return new FiatPaymentMethod(customName);
    }


    private FiatPaymentMethod(FiatPaymentRail paymentRail) {
        super(paymentRail);
    }

    private FiatPaymentMethod(String customName) {
        super(customName);
    }

    @Override
    public bisq.account.protobuf.PaymentMethod.Builder getBuilder(boolean serializeForHash) {
        return getPaymentMethodBuilder(serializeForHash).setFiatPaymentMethod(bisq.account.protobuf.FiatPaymentMethod.newBuilder());
    }

    @Override
    public bisq.account.protobuf.PaymentMethod toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }
    public static FiatPaymentMethod fromProto(bisq.account.protobuf.PaymentMethod proto) {
        return FiatPaymentMethodUtil.getPaymentMethod(proto.getName());
    }

    @Override
    protected FiatPaymentRail getCustomPaymentRail() {
        return FiatPaymentRail.CUSTOM;
    }

    @Override
    public List<TradeCurrency> getTradeCurrencies() {
        return paymentRail.getTradeCurrencies();
    }
}
