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
import bisq.account.bisq1_import.crypto.ImportMoneroAccountParser;
import bisq.account.bisq1_import.crypto.ImportOtherCryptoAssetAccountParser;
import bisq.account.bisq1_import.fiat.ImportAchTransferAccountParser;
import bisq.account.bisq1_import.fiat.ImportAmazonGiftCardAccountParser;
import bisq.account.bisq1_import.fiat.ImportBizumAccountParser;
import bisq.account.bisq1_import.fiat.ImportCashByMailAccountParser;
import bisq.account.bisq1_import.fiat.ImportCashDepositAccountParser;
import bisq.account.bisq1_import.fiat.ImportDomesticWireTransferAccountParser;
import bisq.account.bisq1_import.fiat.ImportF2FAccountParser;
import bisq.account.bisq1_import.fiat.ImportFasterPaymentsAccountParser;
import bisq.account.bisq1_import.fiat.ImportHalCashAccountParser;
import bisq.account.bisq1_import.fiat.ImportInteracETransferAccountParser;
import bisq.account.bisq1_import.fiat.ImportMoneyBeamAccountParser;
import bisq.account.bisq1_import.fiat.ImportMoneyGramAccountParser;
import bisq.account.bisq1_import.fiat.ImportNationalBankAccountParser;
import bisq.account.bisq1_import.fiat.ImportPayIdAccountParser;
import bisq.account.bisq1_import.fiat.ImportPixAccountParser;
import bisq.account.bisq1_import.fiat.ImportPromptPayAccountParser;
import bisq.account.bisq1_import.fiat.ImportRevolutAccountParser;
import bisq.account.bisq1_import.fiat.ImportSameBankAccountParser;
import bisq.account.bisq1_import.fiat.ImportSepaAccountParser;
import bisq.account.bisq1_import.fiat.ImportSepaInstantAccountParser;
import bisq.account.bisq1_import.fiat.ImportStrikeAccountParser;
import bisq.account.bisq1_import.fiat.ImportSwishAccountParser;
import bisq.account.bisq1_import.fiat.ImportUSPostalMoneyOrderAccountParser;
import bisq.account.bisq1_import.fiat.ImportUpholdAccountParser;
import bisq.account.bisq1_import.fiat.ImportUpiAccountParser;
import bisq.account.bisq1_import.fiat.ImportWiseAccountParser;
import bisq.account.bisq1_import.fiat.ImportWiseUsdAccountParser;
import bisq.account.bisq1_import.fiat.ImportZelleAccountParser;
import bisq.common.application.Service;
import bisq.common.json.JsonMapperProvider;
import bisq.security.keys.KeyGeneration;
import com.fasterxml.jackson.databind.JsonNode;
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
public class ImportBisq1AccountsParser implements Service {
    public static List<Account<?, ?>> parseAccounts(String json) {
        try {
            JsonNode root = JsonMapperProvider.get().readTree(json);
            String privateDsaSignatureKeyAsBase64 = requireText(root, "privateDsaSignatureKeyAsBase64");
            byte[] privateDsaSignatureKey = Base64.getDecoder().decode(privateDsaSignatureKeyAsBase64);
            PrivateKey privateDsaKey = KeyGeneration.generatePrivate(privateDsaSignatureKey, KeyGeneration.DSA);

            String publicDsaSignatureKeyAsBase64 = requireText(root, "publicDsaSignatureKeyAsBase64");
            byte[] publicDsaSignatureKey = Base64.getDecoder().decode(publicDsaSignatureKeyAsBase64);
            PublicKey publicDsaKey = KeyGeneration.generatePublic(publicDsaSignatureKey, KeyGeneration.DSA);

            KeyPair dsaKeyPair = new KeyPair(publicDsaKey, privateDsaKey);

            List<Account<?, ?>> accounts = new ArrayList<>();
            for (JsonNode node : root.path("accounts")) {
                try {
                    Account<?, ?> account = parseAccount(node, dsaKeyPair);
                    if (account != null) {
                        accounts.add(account);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse account from Bisq 1 import data.\nnode={}\n",
                            node.toPrettyString(), e);
                    String accountId = asText(node, "id");
                    String methodId = asText(node.path("paymentMethod"), "id");
                    log.warn("Failed to parse account from Bisq 1 import data. accountId={}, paymentMethodId={}",
                            accountId, methodId, e);
                }
            }
            return accounts;
        } catch (Exception e) {
            log.error("importBisq1AccountData failed", e);
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private static Account<?, ?> parseAccount(JsonNode accountNode, KeyPair dsaKeyPair) {
        String paymentMethodId = asText(accountNode.path("paymentMethod"), "id");
        if (paymentMethodId == null) {
            log.error("paymentMethodId is null, skipping account parsing");
            return null;
        }
        return switch (paymentMethodId) {
            case "ACH_TRANSFER" -> new ImportAchTransferAccountParser(accountNode).parse(dsaKeyPair);
            case "AMAZON_GIFT_CARD" -> new ImportAmazonGiftCardAccountParser(accountNode).parse(dsaKeyPair);
            case "AUSTRALIA_PAYID" -> new ImportPayIdAccountParser(accountNode).parse(dsaKeyPair);
            case "BIZUM" -> new ImportBizumAccountParser(accountNode).parse(dsaKeyPair);
            case "CASH_BY_MAIL" -> new ImportCashByMailAccountParser(accountNode).parse(dsaKeyPair);
            case "CASH_DEPOSIT" -> new ImportCashDepositAccountParser(accountNode).parse(dsaKeyPair);
            // ZELLE was CLEAR_X_CHANGE earlier and in Bisq 1
            case "CLEAR_X_CHANGE" -> new ImportZelleAccountParser(accountNode).parse(dsaKeyPair);
            case "DOMESTIC_WIRE_TRANSFER" -> new ImportDomesticWireTransferAccountParser(accountNode).parse(dsaKeyPair);
            case "F2F" -> new ImportF2FAccountParser(accountNode).parse(dsaKeyPair);
            case "FASTER_PAYMENTS" -> new ImportFasterPaymentsAccountParser(accountNode).parse(dsaKeyPair);
            case "HAL_CASH" -> new ImportHalCashAccountParser(accountNode).parse(dsaKeyPair);
            case "INTERAC_E_TRANSFER" -> new ImportInteracETransferAccountParser(accountNode).parse(dsaKeyPair);
            case "MONEY_BEAM" -> new ImportMoneyBeamAccountParser(accountNode).parse(dsaKeyPair);
            case "MONEY_GRAM" -> new ImportMoneyGramAccountParser(accountNode).parse(dsaKeyPair);
            case "NATIONAL_BANK" -> new ImportNationalBankAccountParser(accountNode).parse(dsaKeyPair);
            case "PIX" -> new ImportPixAccountParser(accountNode).parse(dsaKeyPair);
            case "PROMPT_PAY" -> new ImportPromptPayAccountParser(accountNode).parse(dsaKeyPair);
            case "REVOLUT" -> new ImportRevolutAccountParser(accountNode).parse(dsaKeyPair);
            case "SAME_BANK" -> new ImportSameBankAccountParser(accountNode).parse(dsaKeyPair);
            case "SEPA" -> new ImportSepaAccountParser(accountNode).parse(dsaKeyPair);
            case "SEPA_INSTANT" -> new ImportSepaInstantAccountParser(accountNode).parse(dsaKeyPair);
            case "STRIKE" -> new ImportStrikeAccountParser(accountNode).parse(dsaKeyPair);
            case "SWISH" -> new ImportSwishAccountParser(accountNode).parse(dsaKeyPair);
            case "TRANSFERWISE" -> new ImportWiseAccountParser(accountNode).parse(dsaKeyPair);
            case "TRANSFERWISE_USD" -> new ImportWiseUsdAccountParser(accountNode).parse(dsaKeyPair);
            case "UPHOLD" -> new ImportUpholdAccountParser(accountNode).parse(dsaKeyPair);
            case "UPI" -> new ImportUpiAccountParser(accountNode).parse(dsaKeyPair);
            case "US_POSTAL_MONEY_ORDER" -> new ImportUSPostalMoneyOrderAccountParser(accountNode).parse(dsaKeyPair);

            case "BLOCK_CHAINS" -> {
                JsonNode selectedTradeCurrencyNode = ImportAccountParser.requireNode(accountNode, "selectedTradeCurrency");
                String code = ImportAccountParser.requireText(selectedTradeCurrencyNode, "code");
                if (code.equals("XMR")) {
                    yield new ImportMoneroAccountParser(accountNode, false).parse(dsaKeyPair);
                } else {
                    yield new ImportOtherCryptoAssetAccountParser(accountNode, false).parse(dsaKeyPair);
                }
            }
            case "BLOCK_CHAINS_INSTANT" -> {
                JsonNode selectedTradeCurrencyNode = ImportAccountParser.requireNode(accountNode, "selectedTradeCurrency");
                String code = ImportAccountParser.requireText(selectedTradeCurrencyNode, "code");
                if (code.equals("XMR")) {
                    yield new ImportMoneroAccountParser(accountNode, true).parse(dsaKeyPair);
                } else {
                    yield new ImportOtherCryptoAssetAccountParser(accountNode, true).parse(dsaKeyPair);
                }
            }

            default -> {
                log.warn("Import for paymentMethod {} is not yet supported.", paymentMethodId);
                yield null;
            }
        };
    }

    @Nullable
    private static String asText(JsonNode node, String field) {
        JsonNode jsonNode = node.get(field);
        return jsonNode == null || jsonNode.isNull() ? null : jsonNode.asText();
    }

    private static String requireText(JsonNode node, String field) {
        String value = asText(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return value;
    }
}
