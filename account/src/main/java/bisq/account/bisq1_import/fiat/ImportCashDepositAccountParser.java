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

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.CashDepositAccount;
import bisq.account.accounts.fiat.CashDepositAccountPayload;
import bisq.account.accounts.fiat.BankAccountType;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.timestamp.KeyAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;

@Slf4j
public final class ImportCashDepositAccountParser extends ImportBankAccountParser<FiatPaymentMethod, CashDepositAccountPayload> {

    public ImportCashDepositAccountParser(JsonNode accountNode) {
        super(accountNode);
    }

    @Override
    public CashDepositAccount parse(KeyPair dsaKeyPair) {
        String holderName = requireText(paymentAccountPayloadNode, "holderName");
        String bankName = requireText(paymentAccountPayloadNode, "bankName");
        String selectedCurrencyCode = requireSingleTradeCurrencyCode();
        Optional<String> holderTaxIdValue = Optional.ofNullable(holderTaxId);
        Optional<String> bankIdValue = Optional.ofNullable(bankId);
        Optional<String> branchIdValue = Optional.ofNullable(branchId);
        Optional<BankAccountType> accountTypeValue = Optional.ofNullable(accountType);
        Optional<String> nationalAccountIdValue = Optional.ofNullable(nationalAccountId);
        Optional<String> requirements = Optional.ofNullable(asOptionalText(paymentAccountPayloadNode, "requirements"));

        CashDepositAccountPayload accountPayload = new CashDepositAccountPayload(
                paymentAccountPayloadId,
                salt,
                countryCode,
                selectedCurrencyCode,
                holderName,
                holderTaxIdValue,
                bankName,
                bankIdValue,
                branchIdValue,
                accountNr,
                accountTypeValue,
                nationalAccountIdValue,
                requirements);
        accountPayload.verify();

        return new CashDepositAccount(id,
                creationDate,
                accountName,
                accountPayload,
                dsaKeyPair,
                KeyAlgorithm.DSA,
                AccountOrigin.BISQ1_IMPORTED);
    }
}
