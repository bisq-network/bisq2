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

package bisq.offer.options;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.encoding.Hex;
import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class OfferOptionUtil {
    public static List<OfferOption> fromTradeTermsAndReputationScore(String makersTradeTerms,
                                                                     long requiredTotalReputationScore) {
        List<OfferOption> offerOptions = new ArrayList<>();
        if (makersTradeTerms != null && !makersTradeTerms.isEmpty()) {
            offerOptions.add(new TradeTermsOption(makersTradeTerms));
        }
        if (requiredTotalReputationScore > 0) {
            offerOptions.add(new ReputationOption(requiredTotalReputationScore));
        }
        return offerOptions;
    }

    public static List<OfferOption> fromTradeTerms(String makersTradeTerms) {
        List<OfferOption> offerOptions = new ArrayList<>();
        if (makersTradeTerms != null && !makersTradeTerms.isEmpty()) {
            offerOptions.add(new TradeTermsOption(makersTradeTerms));
        }
        return offerOptions;
    }

    public static Optional<TradeTermsOption> findTradeTermsOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof TradeTermsOption)
                .map(option -> (TradeTermsOption) option)
                .findAny();
    }

    public static Optional<ReputationOption> findReputationOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof ReputationOption)
                .map(option -> (ReputationOption) option)
                .findAny();
    }

    public static Optional<CollateralOption> findCollateralOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof CollateralOption)
                .map(option -> (CollateralOption) option)
                .findAny();
    }

    public static Optional<FiatPaymentOption> findFiatPaymentOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof FiatPaymentOption)
                .map(option -> (FiatPaymentOption) option)
                .findAny();
    }

    public static Optional<FeeOption> findFeeOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof FeeOption)
                .map(option -> (FeeOption) option)
                .findAny();
    }

    public static Optional<String> findMakersTradeTerms(Collection<OfferOption> offerOptions) {
        return OfferOptionUtil.findTradeTermsOption(offerOptions).stream().findAny()
                .map(TradeTermsOption::getMakersTradeTerms);
    }

    public static Set<AccountOption> findAccountOptions(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(offerOption -> offerOption instanceof AccountOption)
                .map(offerOption -> (AccountOption) offerOption)
                .collect(Collectors.toSet());
    }

    // Account ID stays private to user. We use offerId for hashing so that it's always a new string in each offer.
    // The account ID is added to the offer so that maker knows which account was assigned once a taker takes the offer.
    public static String createdSaltedAccountId(String accountId, String offerId) {
        String input = accountId + offerId;
        log.error("createdSaltedAccountId accountId={}; offerId={}", accountId, offerId);
        byte[] hash = DigestUtil.hash(input.getBytes(StandardCharsets.UTF_8));
        log.error("createdSaltedAccountId Hex.encode(hash)={}", Hex.encode(hash));
        return Hex.encode(hash);
    }

    public static Optional<Account<? extends PaymentMethod<?>, ?>> findAccountFromSaltedAccountId(Set<Account<? extends PaymentMethod<?>, ?>> accounts,
                                                                                                  String saltedAccountId,
                                                                                                  String offerId) {
        Set<Account<? extends PaymentMethod<?>, ?>> accountSet = accounts.stream()
                .filter(account -> {
                    String salted = createdSaltedAccountId(account.getId(), offerId);
                    log.error("findAccountFromSaltedAccountId accountId={}; offerId={}", account.getId(), offerId);
                    log.error("findAccountFromSaltedAccountId \n{}\n{}", salted, saltedAccountId);

                    return saltedAccountId.equals(salted);
                })
                .collect(Collectors.toSet());
        checkArgument(accountSet.size() <= 1, "findAccountFromSaltedAccountId is expected to return 0 or 1 accounts");
        return accountSet.stream().findAny();
    }
}