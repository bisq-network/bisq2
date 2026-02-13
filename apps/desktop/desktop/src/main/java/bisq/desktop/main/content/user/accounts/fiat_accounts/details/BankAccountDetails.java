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

package bisq.desktop.main.content.user.accounts.fiat_accounts.details;

import bisq.account.accounts.fiat.BankAccount;
import bisq.account.accounts.fiat.BankAccountPayload;
import bisq.account.accounts.util.BankAccountUtils;
import bisq.account.timestamp.AccountTimestampService;
import bisq.i18n.Res;
import javafx.scene.control.Label;

public abstract class BankAccountDetails<P extends BankAccountPayload, T extends BankAccount<P>> extends FiatAccountDetails<T> {
    public BankAccountDetails(T account, AccountTimestampService accountTimestampService) {
        super(account, accountTimestampService);
    }

    @Override
    protected void addDetails() {
        addBankAccountDetails();

        super.addDetails();
    }

    protected void addBankAccountDetails() {
        P accountPayload = account.getAccountPayload();
        String countryCode = accountPayload.getCountryCode();

        addHolderName(accountPayload);
        addHolderId(accountPayload, countryCode);
        addNationalAccountId(accountPayload, countryCode);

        addAccountNr(countryCode, accountPayload);
        addBankAccountType(accountPayload);

        addBankName(accountPayload);
        addBankId(accountPayload, countryCode);
        addBranchId(accountPayload, countryCode);
    }

    protected void addHolderName(P accountPayload) {
        accountPayload.getHolderName().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.holderName"), value));
    }

    protected void addNationalAccountId(P accountPayload, String countryCode) {
        accountPayload.getNationalAccountId().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(BankAccountUtils.getNationalAccountIdDescription(countryCode), value));
    }


    protected void addHolderId(P accountPayload, String countryCode) {
        accountPayload.getHolderId().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(BankAccountUtils.getHolderIdDescription(countryCode), value));
    }

    protected Label addAccountNr(String countryCode, P accountPayload) {
        return addDescriptionAndValueWithCopyButton(BankAccountUtils.getAccountNrDescription(countryCode), accountPayload.getAccountNr());
    }

    protected void addBankAccountType(P accountPayload) {
        accountPayload.getBankAccountType().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.bank.bankAccountType"), value.toDisplayString()));
    }


    protected void addBankName(P accountPayload) {
        accountPayload.getBankName().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.bank.bankName"), value));
    }

    protected void addBankId(P accountPayload, String countryCode) {
        accountPayload.getBankId().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(BankAccountUtils.getBankIdDescription(countryCode), value));
    }

    protected void addBranchId(P accountPayload, String countryCode) {
        accountPayload.getBranchId().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(BankAccountUtils.getBranchIdDescription(countryCode), value));
    }
}
