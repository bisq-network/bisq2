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
import bisq.account.accounts.fiat.DomesticWireTransferAccount;
import bisq.account.accounts.fiat.DomesticWireTransferAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.timestamp.KeyAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;

@Slf4j
public final class ImportDomesticWireTransferAccountParser extends ImportBankAccountParser<FiatPaymentMethod, DomesticWireTransferAccountPayload> {

    public ImportDomesticWireTransferAccountParser(JsonNode accountNode) {
        super(accountNode);
    }

    @Override
    public DomesticWireTransferAccount parse(KeyPair dsaKeyPair) {
        String holderName = requireText(paymentAccountPayloadNode, "holderName");
        String holderAddress = requireText(paymentAccountPayloadNode, "holderAddress");
        String bankName = requireText(paymentAccountPayloadNode, "bankName");
        String branchId = requireText(paymentAccountPayloadNode, "branchId");
        DomesticWireTransferAccountPayload accountPayload = new DomesticWireTransferAccountPayload(
                paymentAccountPayloadId,
                salt,
                holderName,
                holderAddress,
                bankName,
                branchId,
                accountNr);

        return new DomesticWireTransferAccount(id,
                creationDate,
                accountName,
                accountPayload,
                dsaKeyPair,
                KeyAlgorithm.DSA,
                AccountOrigin.BISQ1_IMPORTED);
    }
}
