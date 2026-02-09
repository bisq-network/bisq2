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
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.account.bisq1_import.ImportAccountParser;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.timestamp.KeyAlgorithm;
import bisq.common.asset.CryptoAssetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;

@Slf4j
public final class ImportMoneroAccountParser extends ImportAccountParser<CryptoPaymentMethod, MoneroAccountPayload> {

    private final boolean isInstant;

    public ImportMoneroAccountParser(JsonNode accountNode, boolean isInstant) {
        super(accountNode);

        this.isInstant = isInstant;
    }

    @Override
    public MoneroAccount parse(KeyPair dsaKeyPair) {
        String address;
        Optional<String> mainAddress;
        Optional<String> privateViewKey;
        Optional<String> subAddress;
        Optional<Integer> initialSubAddressIndex;
        Optional<Integer> accountIndex;

        JsonNode extraDataNode = requireNode(accountNode, "extraData");
        String useXMmrSubAddressesValue = asOptionalText(extraDataNode, "UseXMmrSubAddresses");
        boolean useSubAddresses = useXMmrSubAddressesValue != null && useXMmrSubAddressesValue.equals("1");
        if (useSubAddresses) {
            String xmrMainAddressValue = requireText(extraDataNode, "XmrMainAddress");
            mainAddress = Optional.of(xmrMainAddressValue);

            String xmrPrivateViewKeyValue = requireText(extraDataNode, "XmrPrivateViewKey");
            privateViewKey = Optional.of(xmrPrivateViewKeyValue);

            String xmrSubAddressValue = requireText(extraDataNode, "XmrSubAddress");
            subAddress = Optional.of(xmrSubAddressValue);

            // The address field is set to the sub address
            address = xmrSubAddressValue;

            String xmrAccountIndexValue = requireText(extraDataNode, "XmrAccountIndex");
            accountIndex = Optional.of(Integer.parseInt(xmrAccountIndexValue));

            String xmrSubAddressIndexValue = requireText(extraDataNode, "XmrSubAddressIndex");
            initialSubAddressIndex = Optional.of(Integer.parseInt(xmrSubAddressIndexValue));
        } else {
            address = requireText(paymentAccountPayloadNode, "address");
            mainAddress = Optional.empty();
            privateViewKey = Optional.empty();
            subAddress = Optional.empty();
            accountIndex = Optional.empty();
            initialSubAddressIndex = Optional.empty();
        }

        // We do not export the autoconf settings. Users can edit that in the account, and
        // we do not want to add dependency to Bisq 1 auto conf settings.
        Optional<Boolean> isAutoConf = Optional.of(false);
        Optional<Integer> autoConfNumConfirmations = Optional.empty();
        Optional<Long> autoConfMaxTradeAmount = Optional.empty();
        Optional<String> autoConfExplorerUrls = Optional.empty();

        MoneroAccountPayload accountPayload = new MoneroAccountPayload(
                paymentAccountPayloadId,
                salt,
                CryptoAssetRepository.XMR.getCode(),
                address,
                isInstant,
                isAutoConf,
                autoConfNumConfirmations,
                autoConfMaxTradeAmount,
                autoConfExplorerUrls,
                useSubAddresses,
                mainAddress,
                privateViewKey,
                subAddress,
                accountIndex,
                initialSubAddressIndex);
        return new MoneroAccount(id,
                creationDate,
                accountName,
                accountPayload,
                dsaKeyPair,
                KeyAlgorithm.DSA,
                AccountOrigin.BISQ1_IMPORTED);
    }
}
