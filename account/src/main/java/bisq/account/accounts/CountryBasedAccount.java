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

import bisq.account.payment_method.PaymentMethod;
import bisq.common.locale.Country;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class CountryBasedAccount<P extends CountryBasedAccountPayload, M extends PaymentMethod<?>> extends Account<P, M> {
    protected final Country country;

    public CountryBasedAccount(String accountName,
                               M paymentMethod,
                               P payload,
                               Country country) {
        super(accountName, paymentMethod, payload);
        this.country = country;
    }

    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CountryBasedAccount.newBuilder()
                .setCountry(country.toProto(serializeForHash));
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        return getAccountBuilder(serializeForHash)
                .setCountryBasedAccount(toCountryBasedAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.CountryBasedAccount toCountryBasedAccountProto(boolean serializeForHash) {
        return resolveBuilder(getCountryBasedAccountBuilder(serializeForHash), serializeForHash).build();
    }

    public static CountryBasedAccount<?, ?> fromProto(bisq.account.protobuf.Account proto) {
        return switch (proto.getCountryBasedAccount().getMessageCase()) {
            case BANKACCOUNT -> BankAccount.fromProto(proto);
            case SEPAACCOUNT -> SepaAccount.fromProto(proto);
            case F2FACCOUNT -> F2FAccount.fromProto(proto);
            case PIXACCOUNT -> PixAccount.fromProto(proto);
            case STRIKEACCOUNT -> StrikeAccount.fromProto(proto);
            case AMAZONGIFTCARDACCOUNT -> AmazonGiftCardAccount.fromProto(proto);
            case UPIACCOUNT -> UpiAccount.fromProto(proto);
            case BIZUMACCOUNT -> BizumAccount.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}