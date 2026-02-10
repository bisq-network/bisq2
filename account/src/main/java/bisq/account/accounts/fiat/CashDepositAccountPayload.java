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

package bisq.account.accounts.fiat;

import bisq.account.accounts.AccountUtils;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class CashDepositAccountPayload extends BankAccountPayload {
    public static final int REQUIREMENTS_MAX_LENGTH = 150;
    private final Optional<String> requirements;

    public CashDepositAccountPayload(String id,
                                     String countryCode,
                                     String selectedCurrencyCode,
                                     String holderName,
                                     Optional<String> holderTaxId,
                                     String bankName,
                                     Optional<String> bankId,
                                     Optional<String> branchId,
                                     String accountNr,
                                     Optional<BankAccountType> bankAccountType,
                                     Optional<String> nationalAccountId,
                                     Optional<String> requirements) {
        this(id,
                AccountUtils.generateSalt(),
                countryCode,
                selectedCurrencyCode,
                holderName,
                holderTaxId,
                bankName,
                bankId,
                branchId,
                accountNr,
                bankAccountType,
                nationalAccountId,
                requirements);
    }

    public CashDepositAccountPayload(String id,
                                      byte[] salt,
                                      String countryCode,
                                      String selectedCurrencyCode,
                                      String holderName,
                                      Optional<String> holderTaxId,
                                      String bankName,
                                      Optional<String> bankId,
                                      Optional<String> branchId,
                                      String accountNr,
                                      Optional<BankAccountType> bankAccountType,
                                      Optional<String> nationalAccountId,
                                      Optional<String> requirements) {
        super(id,
                salt,
                countryCode,
                selectedCurrencyCode,
                Optional.of(holderName),
                holderTaxId,
                Optional.of(bankName),
                bankId,
                branchId,
                accountNr,
                bankAccountType,
                nationalAccountId);
        this.requirements = requirements;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateText(requirements, REQUIREMENTS_MAX_LENGTH);
    }

    @Override
    protected bisq.account.protobuf.BankAccountPayload.Builder getBankAccountPayloadBuilder(boolean serializeForHash) {
        return super.getBankAccountPayloadBuilder(serializeForHash).setCashDepositAccountPayload(
                toCashDepositAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.CashDepositAccountPayload toCashDepositAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getCashDepositAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.CashDepositAccountPayload.Builder getCashDepositAccountPayloadBuilder(boolean serializeForHash) {
        bisq.account.protobuf.CashDepositAccountPayload.Builder builder = bisq.account.protobuf.CashDepositAccountPayload.newBuilder();
        requirements.ifPresent(builder::setRequirements);
        return builder;
    }

    public static CashDepositAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        var cashDepositAccountPayload = bankAccountPayload.getCashDepositAccountPayload();
        checkArgument(bankAccountPayload.hasBankName(), "Bank name for Cash Deposit must be present");
        return new CashDepositAccountPayload(proto.getId(),
                proto.getSalt().toByteArray(),
                countryBasedPaymentAccountPayload.getCountryCode(),
                bankAccountPayload.getSelectedCurrencyCode(),
                bankAccountPayload.getHolderName(),
                bankAccountPayload.hasHolderId() ? Optional.of(bankAccountPayload.getHolderId()) : Optional.empty(),
                bankAccountPayload.getBankName(),
                bankAccountPayload.hasBankId() ? Optional.of(bankAccountPayload.getBankId()) : Optional.empty(),
                bankAccountPayload.hasBranchId() ? Optional.of(bankAccountPayload.getBranchId()) : Optional.empty(),
                bankAccountPayload.getAccountNr(),
                bankAccountPayload.hasBankAccountType() ? Optional.of(BankAccountType.fromProto(bankAccountPayload.getBankAccountType())) : Optional.empty(),
                bankAccountPayload.hasNationalAccountId() ? Optional.of(bankAccountPayload.getNationalAccountId()) : Optional.empty(),
                cashDepositAccountPayload.hasRequirements() ? Optional.of(cashDepositAccountPayload.getRequirements()) : Optional.empty());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.CASH_DEPOSIT);
    }

    @Override
    public String getAccountDataDisplayString() {
        AccountDataDisplayStringBuilder builder = new AccountDataDisplayStringBuilder();
        holderName.ifPresent(value -> builder.add(Res.get("paymentAccounts.holderName"), value));
        holderId.ifPresent(value -> builder.add(BankAccountUtils.getHolderIdDescription(countryCode), value));
        bankName.ifPresent(value -> builder.add(Res.get("paymentAccounts.bank.bankName"), value));
        bankId.ifPresent(value -> builder.add(BankAccountUtils.getBankIdDescription(countryCode), value));
        branchId.ifPresent(value -> builder.add(BankAccountUtils.getBranchIdDescription(countryCode), value));
        builder.add(BankAccountUtils.getAccountNrDescription(countryCode), accountNr);
        bankAccountType.ifPresent(value -> builder.add(Res.get("paymentAccounts.bank.bankAccountType"),
                Res.get("paymentAccounts.bank.bankAccountType." + value.name())));
        nationalAccountId.ifPresent(value -> builder.add(BankAccountUtils.getNationalAccountIdDescription(countryCode), value));
        requirements.ifPresent(value -> builder.add(Res.get("paymentAccounts.cashDeposit.requirements"), value));
        return builder.toString();
    }
}
