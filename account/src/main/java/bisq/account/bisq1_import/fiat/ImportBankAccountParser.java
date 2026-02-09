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

package bisq.account.bisq1_import.fiat;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.fiat.BankAccountType;
import bisq.account.payment_method.PaymentMethod;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
abstract class ImportBankAccountParser<M extends PaymentMethod<?>, P extends AccountPayload<M>> extends ImportCountryBasedAccountParser<M, P> {
    @Nullable
    protected final String bankName;
    @Nullable
    protected final String branchId;
    protected final String accountNr;
    @Nullable
    protected final BankAccountType accountType;
    @Nullable
    protected final String holderTaxId;
    @Nullable
    protected final String bankId;
    @Nullable
    protected final String nationalAccountId;

    ImportBankAccountParser(JsonNode accountNode) {
        super(accountNode);
        bankName = asOptionalText(paymentAccountPayloadNode, "bankName");
        branchId = asOptionalText(paymentAccountPayloadNode, "branchId");
        accountNr = requireText(paymentAccountPayloadNode, "accountNr");
        holderTaxId = asOptionalText(paymentAccountPayloadNode, "holderTaxId");
        bankId = asOptionalText(paymentAccountPayloadNode, "bankId");
        nationalAccountId = asOptionalText(paymentAccountPayloadNode, "nationalAccountId");

        // Bisq 1 used the translated string for the BankAccountType.
        // The BankAccountType is used only in USA, Canada and Brazil, thus we check for the languages usually used there
        // English
        // payment.checking=Checking
        // payment.savings=Savings
        // Brazil Portuguese
        // payment.checking=Conta Corrente
        // payment.savings=Poupança
        // French (Canada)
        // payment.checking=Vérification
        // payment.savings=Épargne
        String accountTypeI18n = asText(paymentAccountPayloadNode, "accountType");
        if (accountTypeI18n == null || accountTypeI18n.isBlank()) {
            accountType = null;
        } else if (accountTypeI18n.equals("Checking") ||
                accountTypeI18n.equals("Conta Corrente") ||
                accountTypeI18n.equals("Vérification")) {
            accountType = BankAccountType.CHECKING;
        } else {
            accountType = BankAccountType.SAVINGS;
        }
    }
}
