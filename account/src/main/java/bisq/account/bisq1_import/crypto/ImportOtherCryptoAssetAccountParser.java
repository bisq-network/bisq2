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

package bisq.account.bisq1_import.crypto;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.crypto.OtherCryptoAssetAccount;
import bisq.account.accounts.crypto.OtherCryptoAssetAccountPayload;
import bisq.account.bisq1_import.ImportAccountParser;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.timestamp.KeyAlgorithm;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;

@Slf4j
public final class ImportOtherCryptoAssetAccountParser extends ImportAccountParser<CryptoPaymentMethod, OtherCryptoAssetAccountPayload> {

    private final boolean isInstant;

    public ImportOtherCryptoAssetAccountParser(JsonNode accountNode, boolean isInstant) {
        super(accountNode);

        this.isInstant = isInstant;
    }

    @Override
    public OtherCryptoAssetAccount parse(KeyPair dsaKeyPair) {
        JsonNode selectedTradeCurrencyNode = requireNode(accountNode, "selectedTradeCurrency");
        String code = requireText(selectedTradeCurrencyNode, "code");
        String address = requireText(paymentAccountPayloadNode, "address");

        OtherCryptoAssetAccountPayload accountPayload = new OtherCryptoAssetAccountPayload(
                paymentAccountPayloadId,
                salt,
                code,
                address,
                isInstant,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        return new OtherCryptoAssetAccount(id,
                creationDate,
                accountName,
                accountPayload,
                dsaKeyPair,
                KeyAlgorithm.DSA,
                AccountOrigin.BISQ1_IMPORTED);
    }
}
