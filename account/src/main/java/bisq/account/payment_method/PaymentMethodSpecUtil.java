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

import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.crypto.CryptoPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentMethodUtil;
import bisq.common.asset.Asset;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentMethodSpecUtil {
    public static BitcoinPaymentMethod getBitcoinPaymentMethod(String paymentMethod) {
        return BitcoinPaymentMethodUtil.getPaymentMethod(paymentMethod);
    }

    public static FiatPaymentMethod getFiatPaymentMethod(String paymentMethod) {
        return FiatPaymentMethodUtil.getPaymentMethod(paymentMethod);
    }


    public static List<BitcoinPaymentMethodSpec> createBitcoinPaymentMethodSpecs(List<BitcoinPaymentMethod> paymentMethods) {
        return paymentMethods.stream()
                .map(BitcoinPaymentMethodSpec::new)
                .collect(Collectors.toList());
    }

    public static List<BitcoinPaymentMethodSpec> createBitcoinMainChainPaymentMethodSpec() {
        return createBitcoinPaymentMethodSpecs(Collections.singletonList(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)));
    }

    public static List<FiatPaymentMethodSpec> createFiatPaymentMethodSpecs(List<FiatPaymentMethod> paymentMethods) {
        return paymentMethods.stream()
                .map(FiatPaymentMethodSpec::new)
                .collect(Collectors.toList());
    }

   /* public static List<NationalCurrencyPaymentMethod<?>> createNationalCurrencyPaymentMethodSpecs(List<NationalCurrencyPaymentMethod<?>> paymentMethods) {
        return paymentMethods.stream()
                .map(paymentMethod->{
                    if(paymentMethod instanceof FiatPaymentMethod fiatPaymentMethod){
                        return new FiatPaymentMethodSpec(fiatPaymentMethod);
                    }else  if(paymentMethod instanceof StablecoinPaymentMethod stablecoinPaymentMethod){
                        return new StablecoinPaymentMethodSpec(stablecoinPaymentMethod);

                    }
                })
                .collect(Collectors.toList());
    }*/

    public static <M extends PaymentMethod<?>, T extends PaymentMethodSpec<M>> List<M> getPaymentMethods(Collection<T> paymentMethodSpecs) {
        return paymentMethodSpecs.stream()
                .map(PaymentMethodSpec::getPaymentMethod)
                .collect(Collectors.toList());
    }

    public static List<String> getPaymentMethodNames(Collection<? extends PaymentMethodSpec<?>> paymentMethodSpecs) {
        return paymentMethodSpecs.stream().map(PaymentMethodSpec::getPaymentMethodName).collect(Collectors.toList());
    }

    public static List<PaymentMethodSpec<?>> createPaymentMethodSpecs(List<PaymentMethod<?>> paymentMethods,
                                                                      String currencyCode) {
        if (Asset.isFiat(currencyCode)) {
            return paymentMethods.stream()
                    .filter(e -> e instanceof FiatPaymentMethod)
                    .map(e -> (FiatPaymentMethod) e)
                    .map(FiatPaymentMethodSpec::new)
                    .collect(Collectors.toList());
        } else if (Asset.isAltcoin(currencyCode)) {
            return paymentMethods.stream()
                    .filter(e -> e instanceof CryptoPaymentMethod)
                    .map(e -> (CryptoPaymentMethod) e)
                    .map(CryptoPaymentMethodSpec::new)
                    .collect(Collectors.toList());
        } else {
            throw new UnsupportedOperationException("createPaymentMethodSpecs only supports fiat and altcoins. CurrencyCode: " + currencyCode);
        }
    }

    public static PaymentMethodSpec<?> createPaymentMethodSpec(PaymentMethod<?> paymentMethod,
                                                                String currencyCode) {
        if (Asset.isFiat(currencyCode) && paymentMethod instanceof FiatPaymentMethod fiatPaymentMethod) {
            return new FiatPaymentMethodSpec(fiatPaymentMethod);
        } else if (Asset.isAltcoin(currencyCode) && paymentMethod instanceof CryptoPaymentMethod cryptoPaymentMethod) {
            return new CryptoPaymentMethodSpec(cryptoPaymentMethod);
        } else {
            throw new UnsupportedOperationException("createPaymentMethodSpecs only supports fiat and altcoins. CurrencyCode: " + currencyCode);
        }
    }
}