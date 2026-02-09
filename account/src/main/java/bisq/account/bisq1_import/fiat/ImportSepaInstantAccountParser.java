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
import bisq.account.accounts.fiat.SepaInstantAccount;
import bisq.account.accounts.fiat.SepaInstantAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.timestamp.KeyAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Slf4j
public final class ImportSepaInstantAccountParser extends ImportCountryBasedAccountParser<FiatPaymentMethod, SepaInstantAccountPayload> {

    public ImportSepaInstantAccountParser(JsonNode accountNode) {
        super(accountNode);
    }

    @Override
    public SepaInstantAccount parse(KeyPair dsaKeyPair) {
        String holderName = requireText(paymentAccountPayloadNode, "holderName");
        String bic = requireText(paymentAccountPayloadNode, "bic");
        String iban = requireText(paymentAccountPayloadNode, "iban");
        var acceptedCountryCodes = requireStringList(paymentAccountPayloadNode, "acceptedCountryCodes");
        SepaInstantAccountPayload accountPayload = new SepaInstantAccountPayload(
                paymentAccountPayloadId,
                salt,
                holderName,
                iban,
                bic,
                countryCode,
                acceptedCountryCodes);
        accountPayload.verify();
        return new SepaInstantAccount(id,
                creationDate,
                accountName,
                accountPayload,
                dsaKeyPair,
                KeyAlgorithm.DSA,
                AccountOrigin.BISQ1_IMPORTED);
    }
}
