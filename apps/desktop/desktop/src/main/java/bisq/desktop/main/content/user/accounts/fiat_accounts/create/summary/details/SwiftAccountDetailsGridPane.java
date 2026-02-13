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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details;

import bisq.account.accounts.fiat.SwiftAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.i18n.Res;

import java.util.ArrayList;
import java.util.List;

public class SwiftAccountDetailsGridPane extends FiatAccountDetailsGridPane<SwiftAccountPayload> {
    public SwiftAccountDetailsGridPane(SwiftAccountPayload accountPayload, FiatPaymentRail fiatPaymentRail) {
        super(accountPayload, fiatPaymentRail);
    }

    @Override
    protected void addDetails(SwiftAccountPayload accountPayload) {
        List<String> beneficiaryData = new ArrayList<>();
        beneficiaryData.add(accountPayload.getBeneficiaryName());
        beneficiaryData.add(accountPayload.getBeneficiaryAccountNr());
        accountPayload.getBeneficiaryPhone().ifPresent(beneficiaryData::add);
        beneficiaryData.add(accountPayload.getBeneficiaryAddress());
        addDescriptionAndValue(Res.get("paymentAccounts.swift.summary.beneficiaryData"),
                String.join(" / ", beneficiaryData));

        List<String> bankData = new ArrayList<>();
        bankData.add(accountPayload.getBankSwiftCode());
        bankData.add(accountPayload.getBankName());
        accountPayload.getBankBranch().ifPresent(bankData::add);
        bankData.add(accountPayload.getBankAddress());

        addDescriptionAndValue(Res.get("paymentAccounts.swift.summary.bankData"),
                String.join(" / ", bankData));

        List<String> intermediaryParts = new ArrayList<>();
        accountPayload.getIntermediaryBankCountryCode().ifPresent(intermediaryParts::add);
        accountPayload.getIntermediaryBankSwiftCode().ifPresent(intermediaryParts::add);
        accountPayload.getIntermediaryBankName().ifPresent(intermediaryParts::add);
        accountPayload.getIntermediaryBankBranch().ifPresent(intermediaryParts::add);
        accountPayload.getIntermediaryBankAddress().ifPresent(intermediaryParts::add);
        if (!intermediaryParts.isEmpty()) {
            addDescriptionAndValue(Res.get("paymentAccounts.swift.summary.intermediaryBankData"),
                    String.join(" / ", intermediaryParts));
        }
        accountPayload.getAdditionalInstructions().ifPresent(value ->
                addDescriptionAndValue(Res.get("paymentAccounts.swift.additionalInstructions"), value));
    }
}
