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

package bisq.desktop.main.content.user.fiat_accounts.create.summary.details;

import bisq.account.accounts.fiat.BankAccountUtils;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.common.data.Pair;
import bisq.i18n.Res;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NationalBankAccountDetailsGridPane extends FiatAccountDetailsGridPane<NationalBankAccountPayload> {
    public NationalBankAccountDetailsGridPane(NationalBankAccountPayload accountPayload,
                                              FiatPaymentRail fiatPaymentRail) {
        super(accountPayload, fiatPaymentRail);
    }

    protected void addDetails(NationalBankAccountPayload accountPayload) {
        String countryCode = accountPayload.getCountryCode();

        Optional<Pair<String, String>> holderNameDescriptionValue = accountPayload.getHolderName()
                .map(value -> new Pair<>(Res.get("paymentAccounts.holderName"), value));
        Optional<Pair<String, String>> holderIdDescriptionValue = accountPayload.getHolderId()
                .map(value -> new Pair<>(BankAccountUtils.getHolderIdDescriptionShort(countryCode), value));
        if (holderNameDescriptionValue.isPresent() || holderIdDescriptionValue.isPresent()) {
            List<Pair<String, String>> descriptionValuePairs = Stream.of(holderNameDescriptionValue,
                            holderIdDescriptionValue)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            String description = descriptionValuePairs.stream()
                    .map(Pair::getFirst)
                    .collect(Collectors.joining(" / "));
            String value = descriptionValuePairs.stream()
                    .map(Pair::getSecond)
                    .collect(Collectors.joining(" / "));
            addDescriptionAndValue(description, value);
        }

        Optional<Pair<String, String>> bankNameDescriptionValue = accountPayload.getBankName()
                .map(value -> new Pair<>(Res.get("paymentAccounts.bank.bankName"), value));
        Optional<Pair<String, String>> bankIdDescriptionValue = accountPayload.getBankId()
                .map(value -> new Pair<>(BankAccountUtils.getBankIdDescriptionShort(countryCode), value));
        Optional<Pair<String, String>> branchIdDescriptionValue = accountPayload.getBranchId()
                .map(value -> new Pair<>(BankAccountUtils.getBranchIdDescriptionShort(countryCode), value));
        if (bankNameDescriptionValue.isPresent() ||
                bankIdDescriptionValue.isPresent() ||
                branchIdDescriptionValue.isPresent()) {
            List<Pair<String, String>> descriptionValuePairs = Stream.of(bankNameDescriptionValue,
                            bankIdDescriptionValue,
                            branchIdDescriptionValue)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            String description = descriptionValuePairs.stream()
                    .map(Pair::getFirst)
                    .collect(Collectors.joining(" / "));
            String value = descriptionValuePairs.stream()
                    .map(Pair::getSecond)
                    .collect(Collectors.joining(" / "));
            addDescriptionAndValue(description, value);
        }

        Optional<Pair<String, String>> accountNrDescriptionValue = Optional.of(new Pair<>(BankAccountUtils.getAccountNrDescription(countryCode), accountPayload.getAccountNr()));
        Optional<Pair<String, String>> nationalAccountIdDescriptionValue = accountPayload.getNationalAccountId()
                .map(value -> new Pair<>(BankAccountUtils.getNationalAccountIdDescriptionShort(countryCode), value));
        Optional<Pair<String, String>> bankAccountTypeDescriptionValue = accountPayload.getBankAccountType()
                .map(value -> new Pair<>(Res.get("paymentAccounts.bank.bankAccountType"), value.toString()));

        List<Pair<String, String>> descriptionValuePairs = Stream.of(accountNrDescriptionValue,
                        nationalAccountIdDescriptionValue,
                        bankAccountTypeDescriptionValue)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        String description = descriptionValuePairs.stream()
                .map(Pair::getFirst)
                .collect(Collectors.joining(" / "));
        String value = descriptionValuePairs.stream()
                .map(Pair::getSecond)
                .collect(Collectors.joining(" / "));
        addDescriptionAndValue(description, value);
    }
}
