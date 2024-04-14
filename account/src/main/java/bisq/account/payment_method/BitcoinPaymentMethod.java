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

import bisq.common.currency.CryptoCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class BitcoinPaymentMethod extends PaymentMethod<BitcoinPaymentRail> {
    public static BitcoinPaymentMethod fromPaymentRail(BitcoinPaymentRail bitcoinPaymentRail) {
        return new BitcoinPaymentMethod(bitcoinPaymentRail);
    }

    public static BitcoinPaymentMethod fromCustomName(String customName) {
        return new BitcoinPaymentMethod(customName);
    }


    private BitcoinPaymentMethod(BitcoinPaymentRail paymentRail) {
        super(paymentRail);
    }

    private BitcoinPaymentMethod(String name) {
        super(name);
    }

    @Override
    public bisq.account.protobuf.PaymentMethod.Builder getBuilder(boolean serializeForHash) {
        return getPaymentMethodBuilder(serializeForHash).setBitcoinPaymentMethod(bisq.account.protobuf.BitcoinPaymentMethod.newBuilder());
    }

    @Override
    public bisq.account.protobuf.PaymentMethod toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BitcoinPaymentMethod fromProto(bisq.account.protobuf.PaymentMethod proto) {
        return BitcoinPaymentMethodUtil.getPaymentMethod(proto.getName());
    }

    @Override
    protected BitcoinPaymentRail getCustomPaymentRail() {
        return BitcoinPaymentRail.CUSTOM;
    }

    @Override
    public List<TradeCurrency> getTradeCurrencies() {
        return List.of(CryptoCurrencyRepository.BITCOIN);
    }
}
