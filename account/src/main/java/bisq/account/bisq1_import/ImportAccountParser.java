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
import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.encoding.Hex;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class ImportAccountParser<M extends PaymentMethod<?>, P extends AccountPayload<M>> {
    protected final JsonNode accountNode;
    protected final long creationDate;
    protected final String id;
    protected final String accountName;

    protected final JsonNode paymentAccountPayloadNode;
    protected final String paymentAccountPayloadId;
    protected final JsonNode excludeFromJsonDataMapNode;

    protected final byte[] salt;

    protected ImportAccountParser(JsonNode accountNode) {
        this.accountNode = accountNode;

        creationDate = requireLong(accountNode, "creationDate");
        id = requireText(accountNode, "id");
        accountName = requireText(accountNode, "accountName");

        paymentAccountPayloadNode = requireNode(accountNode, "paymentAccountPayload");
        // paymentAccountPayloadId and accountId are the same in Bisq 1
        paymentAccountPayloadId = requireText(paymentAccountPayloadNode, "id");

        excludeFromJsonDataMapNode = paymentAccountPayloadNode.path("excludeFromJsonDataMap");
        String saltAsHex = requireText(excludeFromJsonDataMapNode, "salt");
        salt = Hex.decode(saltAsHex);
    }

    abstract public Account<M , P> parse(KeyPair dsaKeyPair);


    /* --------------------------------------------------------------------- */
    // Json utils
    /* --------------------------------------------------------------------- */

    @Nullable
    protected static String asText(JsonNode node, String field) {
        JsonNode jsonNode = node.get(field);
        return jsonNode == null || jsonNode.isNull() ? null : jsonNode.asText();
    }

    @Nullable
    protected static String asOptionalText(JsonNode node, String field) {
        String value = asText(node, field);
        return value == null || value.isBlank() ? null : value;
    }

    protected static long asLong(JsonNode node, String field) {
        JsonNode jsonNode = node.get(field);
        return jsonNode == null || jsonNode.isNull() ? 0L : jsonNode.asLong();
    }

    public static JsonNode requireNode(JsonNode node, String field) {
        JsonNode jsonNode = node.get(field);
        if (jsonNode == null || jsonNode.isNull()) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return jsonNode;
    }

    protected static String requireText(JsonNode node, String field) {
        String value = asText(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return value;
    }

    protected static String requireTextAllowEmpty(JsonNode node, String field) {
        String value = asText(node, field);
        if (value == null) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return value;
    }

    protected static long requireLong(JsonNode node, String field) {
        JsonNode jsonNode = node.get(field);
        if (jsonNode == null || jsonNode.isNull()) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return jsonNode.asLong();
    }

    protected static String requireExcludeText(JsonNode node, String field) {
        return requireText(node.path("excludeFromJsonDataMap"), field);
    }

    protected static List<String> requireStringList(JsonNode node, String field) {
        JsonNode listNode = requireNode(node, field);
        return asStringList(listNode);
    }

    protected String requireSingleTradeCurrencyCode() {
        String selectedTradeCurrencyCode = getSelectedTradeCurrencyCode();
        if (selectedTradeCurrencyCode != null) {
            return selectedTradeCurrencyCode;
        }
        List<String> tradeCurrencies = requireTradeCurrencyCodes();
        if (tradeCurrencies.size() == 1) {
            return tradeCurrencies.get(0);
        }
        throw new IllegalArgumentException("selectedTradeCurrency is missing and tradeCurrencies is not single-valued");
    }

    protected List<String> requireTradeCurrencyCodes() {
        JsonNode tradeCurrenciesNode = requireNode(accountNode, "tradeCurrencies");
        if (!tradeCurrenciesNode.isArray()) {
            throw new IllegalArgumentException("tradeCurrencies must be an array");
        }
        List<String> codes = new ArrayList<>(tradeCurrenciesNode.size());
        for (JsonNode entry : tradeCurrenciesNode) {
            codes.add(requireTradeCurrencyCode(entry));
        }
        if (codes.isEmpty()) {
            throw new IllegalArgumentException("tradeCurrencies must not be empty");
        }
        return codes;
    }

    @Nullable
    protected String getSelectedTradeCurrencyCode() {
        JsonNode selectedTradeCurrencyNode = accountNode.get("selectedTradeCurrency");
        if (selectedTradeCurrencyNode == null || selectedTradeCurrencyNode.isNull()) {
            return null;
        }
        return requireTradeCurrencyCode(selectedTradeCurrencyNode);
    }

    private static String requireTradeCurrencyCode(JsonNode tradeCurrencyNode) {
        if (tradeCurrencyNode.isTextual()) {
            String value = tradeCurrencyNode.asText();
            if (value.isBlank()) {
                throw new IllegalArgumentException("tradeCurrency code must not be blank");
            }
            return value;
        }
        String code = asText(tradeCurrencyNode, "code");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("tradeCurrency code is missing");
        }
        return code;
    }

    protected static List<String> asStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException("node must be an array");
        }

        List<String> list = new ArrayList<>(node.size());
        for (JsonNode entry : node) {
            if (!entry.isTextual()) {
                throw new IllegalArgumentException("Entry in string list must be a string");
            }
            list.add(entry.asText());
        }
        return list;
    }
}
