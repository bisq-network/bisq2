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

import bisq.account.accounts.fiat.SwiftAccount;
import bisq.account.accounts.fiat.SwiftAccountPayload;
import bisq.account.timestamp.AccountTimestampService;
import bisq.i18n.Res;

public class SwiftAccountDetails extends FiatAccountDetails<SwiftAccount> {
    public SwiftAccountDetails(SwiftAccount account, AccountTimestampService accountTimestampService) {
        super(account, accountTimestampService);
    }

    @Override
    protected void addDetails() {
        SwiftAccountPayload accountPayload = account.getAccountPayload();

        addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.beneficiaryName"),
                accountPayload.getBeneficiaryName());
        addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.beneficiaryAccountNr"),
                accountPayload.getBeneficiaryAccountNr());
        accountPayload.getBeneficiaryPhone().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.beneficiaryPhone"), value));
        addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.beneficiaryAddress"),
                accountPayload.getBeneficiaryAddress());

        addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.bankSwiftCode"),
                accountPayload.getBankSwiftCode());
        addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.bankName"),
                accountPayload.getBankName());
        accountPayload.getBankBranch().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.bankBranch"), value));
        addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.bankAddress"),
                accountPayload.getBankAddress());

        accountPayload.getIntermediaryBankCountryCode().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.intermediaryBankCountry"), value));
        accountPayload.getIntermediaryBankSwiftCode().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.intermediaryBankSwiftCode"), value));
        accountPayload.getIntermediaryBankName().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.intermediaryBankName"), value));
        accountPayload.getIntermediaryBankBranch().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.intermediaryBankBranch"), value));
        accountPayload.getIntermediaryBankAddress().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.intermediaryBankAddress"), value));

        accountPayload.getAdditionalInstructions().ifPresent(value ->
                addDescriptionAndValueWithCopyButton(Res.get("paymentAccounts.swift.additionalInstructions"), value));

        super.addDetails();
    }
}
