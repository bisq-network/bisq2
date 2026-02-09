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
import bisq.account.accounts.fiat.FasterPaymentsAccount;
import bisq.account.accounts.fiat.FasterPaymentsAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.timestamp.KeyAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Slf4j
public final class ImportFasterPaymentsAccountParser extends ImportFiatAccountParser<FiatPaymentMethod, FasterPaymentsAccountPayload> {

    public ImportFasterPaymentsAccountParser(JsonNode accountNode) {
        super(accountNode);
    }

    @Override
    public FasterPaymentsAccount parse(KeyPair dsaKeyPair) {
        String holderName = requireText(excludeFromJsonDataMapNode, "holderName");
        String sortCode = requireText(paymentAccountPayloadNode, "sortCode");
        String accountNr = requireText(paymentAccountPayloadNode, "accountNr");
        FasterPaymentsAccountPayload accountPayload = new FasterPaymentsAccountPayload(
                paymentAccountPayloadId,
                salt,
                holderName,
                sortCode,
                accountNr);
        accountPayload.verify();
        return new FasterPaymentsAccount(id,
                creationDate,
                accountName,
                accountPayload,
                dsaKeyPair,
                KeyAlgorithm.DSA,
                AccountOrigin.BISQ1_IMPORTED);
    }
}
