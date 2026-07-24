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

package bisq.account.payment_method.fiat;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class FiatPaymentMethodUtil {
    public static Optional<FiatPaymentMethod> findPaymentMethod(String name) {
        try {
            FiatPaymentRail paymentRail = FiatPaymentRail.valueOf(name);
            FiatPaymentMethod paymentMethod = FiatPaymentMethod.fromPaymentRail(paymentRail);
            if (!paymentMethod.isCustomPaymentMethod()) {
                return Optional.of(paymentMethod);
            } else {
                return Optional.empty();
            }
        } catch (Throwable ignore) {
            return Optional.empty();
        }
    }

    public static FiatPaymentMethod getPaymentMethod(String name) {
        try {
            FiatPaymentRail paymentRail = FiatPaymentRail.valueOf(name);
            FiatPaymentMethod paymentMethod = FiatPaymentMethod.fromPaymentRail(paymentRail);
            if (!paymentMethod.isCustomPaymentMethod()) {
                return paymentMethod;
            }
        } catch (Throwable throwable) {
            log.debug("Failed to create FiatPaymentRail from name {}. " +
                    "This is expected for custom payment methods", name);
        }
        return FiatPaymentMethod.fromCustomName(name);
    }

    /**
     * Bisq Easy fiat payment method list for the given currency. Excludes CUSTOM (user-defined, not a
     * concrete rail) and the deprecated CASH_APP, but keeps accountless-only rails such as TELE_BIRR —
     * on the Bisq Easy offerbook any payment rail (account-backed or not) is a valid trade term.
     * <p>
     * For standard, account-backed payment methods (desktop account wizard, MuSig, REST payment-accounts
     * API), use {@link #getStandardAccountPaymentMethods(String)} instead.
     */
    public static List<FiatPaymentMethod> getPaymentMethods(String currencyCode) {
        //noinspection deprecation
        return FiatPaymentRailUtil.getPaymentRails(currencyCode).stream()
                .filter(rail -> rail != FiatPaymentRail.CUSTOM)
                .filter(rail -> rail != FiatPaymentRail.CASH_APP)
                .map(FiatPaymentMethod::fromPaymentRail)
                .collect(Collectors.toList());
    }

    /**
     * Standard, account-backed fiat payment methods for the given currency: the subset of
     * {@link #getPaymentMethods(String)} that can back a classic payment account (see
     * {@link FiatPaymentRail#supportsStandardAccountCreation(FiatPaymentRail)}).
     * <p>
     * Use this for the account-creation wizard, MuSig offer flow, and the REST payment-accounts API.
     * Do NOT use it for the Bisq Easy offerbook — that must keep listing accountless-only rails like
     * TELE_BIRR, which this method excludes.
     */
    public static List<FiatPaymentMethod> getStandardAccountPaymentMethods(String currencyCode) {
        return getPaymentMethods(currencyCode).stream()
                .filter(paymentMethod -> FiatPaymentRail.supportsStandardAccountCreation(paymentMethod.getPaymentRail()))
                .collect(Collectors.toList());
    }
}