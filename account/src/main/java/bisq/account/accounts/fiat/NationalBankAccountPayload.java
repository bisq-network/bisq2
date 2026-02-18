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

import bisq.account.accounts.SelectableCurrencyAccountPayload;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class NationalBankAccountPayload extends BankAccountPayload implements SelectableCurrencyAccountPayload {
    public NationalBankAccountPayload(String id,
                                      String countryCode,
                                      String selectedCurrencyCode,
                                      Optional<String> holderName,
                                      Optional<String> holderId,
                                      Optional<String> bankName,
                                      Optional<String> bankId,
                                      Optional<String> branchId,
                                      String accountNr,
                                      Optional<BankAccountType> bankAccountType,
                                      Optional<String> nationalAccountId) {
        this(id,
                AccountUtils.generateSalt(),
                countryCode,
                selectedCurrencyCode,
                holderName,
                holderId,
                bankName,
                bankId,
                branchId,
                accountNr,
                bankAccountType,
                nationalAccountId);
    }

    public NationalBankAccountPayload(String id,
                                       byte[] salt,
                                       String countryCode,
                                       String selectedCurrencyCode,
                                       Optional<String> holderName,
                                       Optional<String> holderId,
                                       Optional<String> bankName,
                                       Optional<String> bankId,
                                       Optional<String> branchId,
                                       String accountNr,
                                       Optional<BankAccountType> bankAccountType,
                                       Optional<String> nationalAccountId) {
        super(id,
                salt,
                countryCode,
                selectedCurrencyCode,
                holderName,
                holderId,
                bankName,
                bankId,
                branchId,
                accountNr,
                bankAccountType,
                nationalAccountId);

        verify();
    }

    @Override
    protected bisq.account.protobuf.BankAccountPayload.Builder getBankAccountPayloadBuilder(boolean serializeForHash) {
        return super.getBankAccountPayloadBuilder(serializeForHash).setNationalBankAccountPayload(
                toNationalBankAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.NationalBankAccountPayload toNationalBankAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getNationalBankAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.NationalBankAccountPayload.Builder getNationalBankAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.NationalBankAccountPayload.newBuilder();
    }

    public static NationalBankAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        return new NationalBankAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
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
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
    }
}
