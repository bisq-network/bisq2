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

package bisq.account.bisq1_import;


import bisq.account.accounts.Account;
import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.accounts.fiat.ZelleAccountPayload;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.util.ByteArrayUtils;
import bisq.security.keys.KeyGeneration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Getter
public class ImportBisq1AccountService implements Service {
    private final ObjectMapper mapper = new ObjectMapper();

    public ImportBisq1AccountService() {
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public List<Account<?, ?>> getAccounts(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String privateDsaSignatureKeyAsBase64 = asText(root, "privateDsaSignatureKeyAsBase64");
            byte[] privateDsaSignatureKey = Base64.getDecoder().decode(privateDsaSignatureKeyAsBase64);
            PrivateKey privateDsaKey = KeyGeneration.generatePrivate(privateDsaSignatureKey, KeyGeneration.DSA);

            String publicDsaSignatureKeyAsBase64 = asText(root, "publicDsaSignatureKeyAsBase64");
            byte[] publicDsaSignatureKey = Base64.getDecoder().decode(publicDsaSignatureKeyAsBase64);
            PublicKey publicDsaKey = KeyGeneration.generatePublic(publicDsaSignatureKey, KeyGeneration.DSA);

            KeyPair dsaKeyPair = new KeyPair(publicDsaKey, privateDsaKey);

            List<Account<?, ?>> accounts = new ArrayList<>();
            for (JsonNode node : root.path("accounts")) {
                Account<?, ?> account = parseAccount(node, dsaKeyPair);
                if (account != null) {
                    accounts.add(account);
                }
            }
            return accounts;
        } catch (Exception e) {
            log.error("importBisq1AccountData failed for json\n{}", json, e);
            throw new RuntimeException(e);
        }
    }

    private Account<?, ?> parseAccount(JsonNode accountNode, KeyPair dsaKeyPair) {
        String paymentMethodId = asText(accountNode.path("paymentMethod"), "id");
        accountNode.path("paymentAccountPayload");
        long creationDate = asLong(accountNode, "creationDate");
        String accountId = asText(accountNode, "id");
        String accountName = asText(accountNode, "accountName");
        JsonNode paymentAccountPayloadNode = accountNode.path("paymentAccountPayload");
        // paymentAccountPayloadId and accountId are the same in Bisq 1
        String paymentAccountPayloadId = asText(paymentAccountPayloadNode, "id");
        String saltAsHex = asText(accountNode.path("extraData"), "saltAsHex");

        byte[] salt = saltAsHex != null ? Hex.decode(saltAsHex) : ByteArrayUtils.getRandomBytes(32);
        switch (paymentMethodId) {
            case "CLEAR_X_CHANGE":
                String emailOrMobileNr = asText(paymentAccountPayloadNode, "emailOrMobileNr");
                String holderName = asText(paymentAccountPayloadNode, "holderName");
                ZelleAccountPayload accountPayload = new ZelleAccountPayload(paymentAccountPayloadId, holderName, emailOrMobileNr, paymentMethodId, salt);
                return new ZelleAccount(accountId, creationDate, accountName, accountPayload, dsaKeyPair, KeyGeneration.DSA);
            default:
                return null;
        }
    }

    @Nullable
    private static String asText(JsonNode node, String field) {
        JsonNode jsonNode = node.get(field);
        return jsonNode == null || jsonNode.isNull() ? null : jsonNode.asText();
    }

    private static long asLong(JsonNode node, String field) {
        JsonNode jsonNode = node.get(field);
        return jsonNode == null || jsonNode.isNull() ? 0L : jsonNode.asLong();
    }
}
