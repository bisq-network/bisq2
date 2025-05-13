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

public abstract class BankAccount<P extends BankAccountPayload> extends CountryBasedAccount<P, FiatPaymentMethod> {
    public BankAccount(String accountName, FiatPaymentMethod paymentMethod, P payload, Country country) {
        super(accountName, paymentMethod, payload, country);
    }

    protected bisq.account.protobuf.BankAccount.Builder getBankAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.BankAccount.newBuilder();
    }

    protected bisq.account.protobuf.BankAccount toBankAccountProto(boolean serializeForHash) {
        return resolveBuilder(getBankAccountBuilder(serializeForHash), serializeForHash).build();
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash)
                .setBankAccount(toBankAccountProto(serializeForHash));
    }

    public static BankAccount<?> fromProto(bisq.account.protobuf.Account proto) {
        return switch (proto.getCountryBasedAccount().getBankAccount().getMessageCase()) {
            case ACHTRANSFERACCOUNT -> AchTransferAccount.fromProto(proto);
            case NATIONALBANKACCOUNT -> NationalBankAccount.fromProto(proto);
            case CASHDEPOSITACCOUNT -> CashDepositAccount.fromProto(proto);
            case SAMEBANKACCOUNT -> SameBankAccount.fromProto(proto);
            case SPECIFICBANKSACCOUNT -> SpecificBanksAccount.fromProto(proto);
            case DOMESTICWIRETRANSFERACCOUNT -> DomesticWireTransferAccount.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}
