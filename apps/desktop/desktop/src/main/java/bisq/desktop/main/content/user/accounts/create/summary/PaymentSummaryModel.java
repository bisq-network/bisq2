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

package bisq.desktop.main.content.user.accounts.create.summary;

import bisq.account.payment_method.PaymentMethod;
import bisq.common.monetary.Monetary;
import bisq.common.util.StringUtils;
import bisq.desktop.common.view.Model;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class PaymentSummaryModel implements Model {
    @Getter
    private Optional<PaymentMethod<?>> paymentMethod = Optional.empty();
    @Getter
    private Optional<String> accountName = Optional.empty();
    @Getter
    private Optional<String> riskLevel = Optional.empty();
    @Getter
    private Optional<Monetary> tradeLimit = Optional.empty();

    @Getter
    private Map<String, Object> accountData = new HashMap<>();

    @Getter
    private Map<String, Object> optionsData = new HashMap<>();

    @Getter
    private Map<String, String> summaryDetails = new LinkedHashMap<>();

    @Setter
    @Getter
    private boolean accountNameManuallyEdited = false;

    private final Map<String, String> fullTextForTooltips = new LinkedHashMap<>();

    public void setAccountData(Map<String, Object> accountData) {
        this.accountData = accountData != null ? new HashMap<>(accountData) : new HashMap<>();
    }

    public void setOptionsData(Map<String, Object> optionsData) {
        this.optionsData = optionsData != null ? new HashMap<>(optionsData) : new HashMap<>();
    }

    public void setSummaryDetails(Map<String, String> summaryDetails) {
        this.summaryDetails = summaryDetails != null ? new LinkedHashMap<>(summaryDetails) : new LinkedHashMap<>();
    }

    public void addFullTextForTooltip(String key, String fullText) {
        if (StringUtils.isNotEmpty(fullText)) {
            fullTextForTooltips.put(key, fullText);
        }
    }

    public Optional<String> getFullTextForTooltip(String key) {
        return Optional.ofNullable(fullTextForTooltips.get(key));
    }

    public void clearTooltipData() {
        fullTextForTooltips.clear();
    }

    public void setPaymentMethod(PaymentMethod<?> method) {
        paymentMethod = Optional.ofNullable(method);
    }

    public void setAccountName(String name) {
        accountName = Optional.ofNullable(name);
    }

    public void setRiskLevel(String level) {
        riskLevel = Optional.ofNullable(level);
    }

    public void setTradeLimit(Monetary limit) {
        tradeLimit = Optional.ofNullable(limit);
    }
}