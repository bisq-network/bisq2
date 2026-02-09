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
import bisq.account.accounts.fiat.WiseUsdAccount;
import bisq.account.accounts.fiat.WiseUsdAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.timestamp.KeyAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Slf4j
public final class ImportWiseUsdAccountParser extends ImportCountryBasedAccountParser<FiatPaymentMethod, WiseUsdAccountPayload> {

    public ImportWiseUsdAccountParser(JsonNode accountNode) {
        super(accountNode);
    }

    @Override
    public WiseUsdAccount parse(KeyPair dsaKeyPair) {
        String email = requireText(paymentAccountPayloadNode, "email");
        String holderName = requireText(paymentAccountPayloadNode, "holderName");
        String beneficiaryAddress = requireText(paymentAccountPayloadNode, "beneficiaryAddress");
        WiseUsdAccountPayload accountPayload = new WiseUsdAccountPayload(
                paymentAccountPayloadId,
                salt,
                countryCode,
                holderName,
                email,
                beneficiaryAddress);
        accountPayload.verify();
        return new WiseUsdAccount(id,
                creationDate,
                accountName,
                accountPayload,
                dsaKeyPair,
                KeyAlgorithm.DSA,
                AccountOrigin.BISQ1_IMPORTED);
    }
}
