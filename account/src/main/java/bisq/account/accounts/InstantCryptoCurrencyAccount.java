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

package bisq.account.accounts;

import bisq.account.payment_method.CryptoPaymentMethod;
import bisq.account.payment_method.CryptoPaymentRail;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class InstantCryptoCurrencyAccount extends Account<InstantCryptoCurrencyAccountPayload, CryptoPaymentMethod> {

    public InstantCryptoCurrencyAccount(String accountName,
                                         CryptoPaymentMethod paymentMethod,
                                         InstantCryptoCurrencyAccountPayload accountPayload) {
        super(accountName, paymentMethod, accountPayload);
    }

    public static CryptoPaymentMethod createPaymentMethod(String currencyCode, CryptoPaymentRail paymentRail) {
        return CryptoPaymentMethod.fromPaymentRail(paymentRail, currencyCode);
    }

    public static String getPaymentMethodName(String currencyCode, CryptoPaymentRail paymentRail) {
        return createPaymentMethod(currencyCode, paymentRail).getName();
    }

    public String getCurrencyCode() {
        return paymentMethod.getCurrencyCode();
    }

    public CryptoPaymentRail getPaymentRail() {
        return paymentMethod.getPaymentRail();
    }

    public String getAddress() {
        return accountPayload.getAddress();
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setInstantCryptoCurrencyAccount(toInstantCryptoCurrencyAccountProto(serializeForHash));
    }

    public static InstantCryptoCurrencyAccount fromProto(bisq.account.protobuf.Account proto) {
        InstantCryptoCurrencyAccountPayload payload = InstantCryptoCurrencyAccountPayload.fromProto(proto.getAccountPayload());

        CryptoPaymentMethod paymentMethod = CryptoPaymentMethod.fromProto(proto.getPaymentMethod());

        return new InstantCryptoCurrencyAccount(
                proto.getAccountName(),
                paymentMethod,
                payload
        );
    }

    private bisq.account.protobuf.InstantCryptoCurrencyAccount toInstantCryptoCurrencyAccountProto(boolean serializeForHash) {
        return resolveBuilder(getInstantCryptoCurrencyAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.InstantCryptoCurrencyAccount.Builder getInstantCryptoCurrencyAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.InstantCryptoCurrencyAccount.newBuilder();
    }
}