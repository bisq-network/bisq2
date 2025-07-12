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

package bisq.desktop.main.content.user.accounts.details;

import bisq.account.accounts.fiat.BankAccountUtils;
import bisq.account.accounts.fiat.NationalBankAccount;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.i18n.Res;

public class NationalBankAccountDetailsVBox extends FiatAccountDetailsVBox<NationalBankAccount> {
    public NationalBankAccountDetailsVBox(NationalBankAccount account) {
        super(account);
    }

    @Override
    protected void addDetails(NationalBankAccount account) {
        NationalBankAccountPayload accountPayload = account.getAccountPayload();
        String countryCode = accountPayload.getCountryCode();

        accountPayload.getHolderName().ifPresent(value -> addDescriptionAndValueWithCopyButton(Res.get("user.paymentAccounts.holderName"), value));
        accountPayload.getHolderId().ifPresent(value -> addDescriptionAndValueWithCopyButton(BankAccountUtils.getHolderIdDescription(countryCode), value));
        accountPayload.getBankName().ifPresent(value -> addDescriptionAndValueWithCopyButton(Res.get("user.paymentAccounts.bank.bankName"), value));
        accountPayload.getBankId().ifPresent(value -> addDescriptionAndValueWithCopyButton(BankAccountUtils.getBankIdDescription(countryCode), value));
        accountPayload.getBranchId().ifPresent(value -> addDescriptionAndValueWithCopyButton(BankAccountUtils.getBranchIdDescription(countryCode), value));
        addDescriptionAndValueWithCopyButton(BankAccountUtils.getAccountNrDescription(countryCode), accountPayload.getAccountNr());
        accountPayload.getNationalAccountId().ifPresent(value -> addDescriptionAndValueWithCopyButton(BankAccountUtils.getNationalAccountIdDescription(countryCode), value));
        accountPayload.getBankAccountType().ifPresent(value -> addDescriptionAndValueWithCopyButton(Res.get("user.paymentAccounts.bank.bankAccountType"), value.toString()));
    }
}
