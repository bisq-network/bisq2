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

import bisq.account.payment_method.FiatPaymentMethod;
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
public abstract class IfscBasedAccount<P extends IfscBasedAccountPayload> extends CountryBasedAccount<P, FiatPaymentMethod> {

    protected IfscBasedAccount(String accountName, FiatPaymentMethod paymentMethod, P payload, Country country) {
        super(accountName, paymentMethod, payload, country);
    }

    protected bisq.account.protobuf.IfscBasedAccount.Builder getIfscBasedAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.IfscBasedAccount.newBuilder();
    }

    protected bisq.account.protobuf.IfscBasedAccount toIfscBasedAccountProto(boolean serializeForHash) {
        return resolveBuilder(getIfscBasedAccountBuilder(serializeForHash), serializeForHash).build();
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash)
                .setIfscBasedAccount(toIfscBasedAccountProto(serializeForHash));
    }

    public static IfscBasedAccount<?> fromProto(bisq.account.protobuf.Account proto) {
        return switch (proto.getCountryBasedAccount().getIfscBasedAccount().getMessageCase()) {
            case NEFTACCOUNT -> NeftAccount.fromProto(proto);
            case RTGSACCOUNT -> RtgsAccount.fromProto(proto);
            case IMPSACCOUNT -> ImpsAccount.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}