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

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SameBankAccountPayload extends BankAccountPayload {
    public SameBankAccountPayload(String id,
                                  String countryCode,
                                  String selectedCurrencyCode,
                                  Optional<String> holderName,
                                  Optional<String> holderTaxId,
                                  Optional<String> bankName,
                                  Optional<String> bankId,
                                  Optional<String> branchId,
                                  String accountNr,
                                  Optional<BankAccountType> bankAccountType,
                                  Optional<String> nationalAccountId) {
        super(id,
                countryCode,
                selectedCurrencyCode,
                holderName,
                holderTaxId,
                bankName,
                bankId,
                branchId,
                accountNr,
                bankAccountType,
                nationalAccountId);
    }

    @Override
    protected bisq.account.protobuf.BankAccountPayload.Builder getBankAccountPayloadBuilder(boolean serializeForHash) {
        return super.getBankAccountPayloadBuilder(serializeForHash).setSameBankAccountPayload(
                toSameBankAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.SameBankAccountPayload toSameBankAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getSameBankAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SameBankAccountPayload.Builder getSameBankAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SameBankAccountPayload.newBuilder();
    }

    public static SameBankAccountPayload fromProto(AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        return new SameBankAccountPayload(proto.getId(),
                countryBasedPaymentAccountPayload.getCountryCode(),
                bankAccountPayload.getSelectedCurrencyCode(),
                bankAccountPayload.hasHolderName() ? Optional.of(bankAccountPayload.getHolderName()) : Optional.empty(),
                bankAccountPayload.hasHolderId() ? Optional.of(bankAccountPayload.getHolderId()) : Optional.empty(),
                bankAccountPayload.hasBankName() ? Optional.of(bankAccountPayload.getBankName()) : Optional.empty(),
                bankAccountPayload.hasBankId() ? Optional.of(bankAccountPayload.getBankId()) : Optional.empty(),
                bankAccountPayload.hasBranchId() ? Optional.of(bankAccountPayload.getBranchId()) : Optional.empty(),
                bankAccountPayload.getAccountNr(),
                bankAccountPayload.hasBankAccountType() ? Optional.of(BankAccountType.fromProto(bankAccountPayload.getBankAccountType())) : Optional.empty(),
                bankAccountPayload.hasNationalAccountId() ? Optional.of(bankAccountPayload.getNationalAccountId()) : Optional.empty());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SAME_BANK);
    }

    @Override
    public String getAccountDataDisplayString() {
        AccountDataDisplayStringBuilder builder = new AccountDataDisplayStringBuilder();
        holderName.ifPresent(value -> builder.add(Res.get("user.paymentAccounts.holderName"), value));
        holderId.ifPresent(value -> builder.add(BankAccountUtils.getHolderIdDescription(countryCode), value));
        bankName.ifPresent(value -> builder.add(Res.get("user.paymentAccounts.bank.bankName"), value));
        bankId.ifPresent(value -> builder.add(BankAccountUtils.getBankIdDescription(countryCode), value));
        branchId.ifPresent(value -> builder.add(BankAccountUtils.getBranchIdDescription(countryCode), value));
        builder.add(BankAccountUtils.getAccountNrDescription(countryCode), accountNr);
        bankAccountType.ifPresent(value -> builder.add(Res.get("user.paymentAccounts.bank.bankAccountType"),
                Res.get("user.paymentAccounts.bank.bankAccountType." + value.name())));
        nationalAccountId.ifPresent(value -> builder.add(BankAccountUtils.getNationalAccountIdDescription(countryCode), value));
        return builder.toString();
    }
}