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
import bisq.account.accounts.fiat.MoneseAccount;
import bisq.account.accounts.fiat.MoneseAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.timestamp.KeyAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.List;

@Slf4j
public final class ImportMoneseAccountParser extends ImportFiatAccountParser<FiatPaymentMethod, MoneseAccountPayload> {

    public ImportMoneseAccountParser(JsonNode accountNode) {
        super(accountNode);
    }

    @Override
    public MoneseAccount parse(KeyPair dsaKeyPair) {
        String holderName = requireText(paymentAccountPayloadNode, "holderName");
        String mobileNr = requireText(paymentAccountPayloadNode, "mobileNr");
        List<String> selectedCurrencyCodes = requireTradeCurrencyCodes();
        MoneseAccountPayload accountPayload = new MoneseAccountPayload(
                paymentAccountPayloadId,
                salt,
                selectedCurrencyCodes,
                holderName,
                mobileNr);

        return new MoneseAccount(id,
                creationDate,
                accountName,
                accountPayload,
                dsaKeyPair,
                KeyAlgorithm.DSA,
                AccountOrigin.BISQ1_IMPORTED);
    }
}
