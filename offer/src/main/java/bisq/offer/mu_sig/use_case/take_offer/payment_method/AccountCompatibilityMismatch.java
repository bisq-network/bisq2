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

package bisq.offer.mu_sig.use_case.take_offer.payment_method;

import bisq.account.accounts.Account;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Why a taker account belonging to an offered payment method is not eligible: the account's
 * value for one compatibility dimension is not within the offer's accepted values. Domain
 * facts only; localized explanation texts are built in the UI layer.
 */
public record AccountCompatibilityMismatch(Account<?, ?> account,
                                           Dimension dimension,
                                           Optional<String> accountValue,
                                           List<String> acceptedValues) {
    public enum Dimension {
        COUNTRY,
        BANK
    }

    public AccountCompatibilityMismatch {
        checkNotNull(account, "account must not be null");
        checkNotNull(dimension, "dimension must not be null");
        checkNotNull(accountValue, "accountValue must not be null");
        acceptedValues = List.copyOf(acceptedValues);
    }
}
