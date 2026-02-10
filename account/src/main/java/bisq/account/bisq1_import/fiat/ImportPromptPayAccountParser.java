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
import bisq.account.accounts.fiat.PromptPayAccount;
import bisq.account.accounts.fiat.PromptPayAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.timestamp.KeyAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Slf4j
public final class ImportPromptPayAccountParser extends ImportCountryBasedAccountParser<FiatPaymentMethod, PromptPayAccountPayload> {

    public ImportPromptPayAccountParser(JsonNode accountNode) {
        super(accountNode);
    }

    @Override
    public PromptPayAccount parse(KeyPair dsaKeyPair) {
        String promptPayId = requireText(paymentAccountPayloadNode, "promptPayId");
        PromptPayAccountPayload accountPayload = new PromptPayAccountPayload(
                paymentAccountPayloadId,
                salt,
                countryCode,
                promptPayId);
        accountPayload.verify();

        return new PromptPayAccount(id,
                creationDate,
                accountName,
                accountPayload,
                dsaKeyPair,
                KeyAlgorithm.DSA,
                AccountOrigin.BISQ1_IMPORTED);
    }
}
