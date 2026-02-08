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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.payment_method;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.util.StringUtils;

import java.util.Comparator;
import java.util.Map;

import static bisq.desktop.main.content.user.accounts.fiat_accounts.create.payment_method.PaymentMethodSelectionView.PaymentMethodItem;

class PaymentMethodComparator implements Comparator<PaymentMethodItem> {
    private final String userCountryCode;
    private final String userCurrencyCode;
    private final Map<FiatPaymentRail, Integer> popularityScoreByFiatPaymentRail = FiatPaymentRailUtil.getPopularityScore();

    public PaymentMethodComparator(String userCountryCode, String userCurrencyCode) {
        this.userCountryCode = StringUtils.toOptional(userCountryCode).orElse("").toUpperCase();
        this.userCurrencyCode = StringUtils.toOptional(userCurrencyCode).orElse("").toUpperCase();
    }

    @Override
    public int compare(PaymentMethodItem a,
                       PaymentMethodItem b) {
        int localeRelevanceA = calculateLocaleRelevance(a);
        int localeRelevanceB = calculateLocaleRelevance(b);

        if (localeRelevanceA != localeRelevanceB) {
            return Integer.compare(localeRelevanceB, localeRelevanceA);
        }

        int popularityA = getPopularityScore(a.getPaymentMethod());
        int popularityB = getPopularityScore(b.getPaymentMethod());

        if (popularityA != popularityB) {
            return Integer.compare(popularityB, popularityA);
        }

        return a.getName().compareToIgnoreCase(b.getName());
    }

    private int calculateLocaleRelevance(PaymentMethodItem item) {
        PaymentMethod<?> method = item.getPaymentMethod();
        int relevanceScore = 0;

        if (method instanceof FiatPaymentMethod fiatMethod) {
            if (fiatMethod.getSupportedCurrencyCodes().contains(userCurrencyCode)) {
                relevanceScore += 2;
            }


            boolean supportsUserCountry = fiatMethod.getSupportedCountries().stream()
                    .anyMatch(country -> country.getCode().equalsIgnoreCase(userCountryCode));
            if (supportsUserCountry) {
                relevanceScore++;
            }
        }

        return relevanceScore;
    }

    private int getPopularityScore(PaymentMethod<?> method) {
        if (method instanceof FiatPaymentMethod fiatMethod) {
            FiatPaymentRail rail = fiatMethod.getPaymentRail();
            return popularityScoreByFiatPaymentRail.getOrDefault(rail, 0);
        }
        return 0;
    }
}
