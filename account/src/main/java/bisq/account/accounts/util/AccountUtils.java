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

package bisq.account.accounts.util;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.crypto.CryptoAssetAccountPayload;
import bisq.account.accounts.fiat.BankAccountPayload;
import bisq.account.accounts.fiat.CashByMailAccountPayload;
import bisq.account.accounts.fiat.CountryBasedAccountPayload;
import bisq.account.accounts.fiat.F2FAccountPayload;
import bisq.account.accounts.fiat.SameBankAccountPayload;
import bisq.account.accounts.fiat.SepaAccountPayload;
import bisq.account.accounts.fiat.SepaInstantAccountPayload;
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.common.util.ByteArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AccountUtils {
    public static byte[] generateSalt() {
        return ByteArrayUtils.getRandomBytes(32);
    }

    public static boolean isStateRequired(String countryCode) {
        return switch (countryCode) {
            case "US", "CA", "AU", "MY", "MX", "CN" -> true;
            default -> false;
        };
    }

    public static List<String> getAcceptedBanks(AccountPayload<?> accountPayload) {
       /* if (account instanceof SpecificBanksAccount) {
            acceptedBanks = new ArrayList<>(((SpecificBanksAccount) account).getAcceptedBanks());
        } else*/
        if (accountPayload instanceof SameBankAccountPayload sameBankAccountPayload &&
                sameBankAccountPayload.getBankId().isPresent()) {
            return Collections.singletonList(sameBankAccountPayload.getBankId().get());
        }
        return List.of();
    }

    public static Optional<String> getBankId(AccountPayload<?> accountPayload) {
        if (accountPayload instanceof BankAccountPayload bankAccountPayload) {
            return bankAccountPayload.getBankId();
        } else {
            return Optional.empty();
        }
    }

    public static Optional<String> getCountryCode(AccountPayload<?> accountPayload) {
        if (accountPayload instanceof CountryBasedAccountPayload countryBasedAccountPayload) {
            return Optional.of(countryBasedAccountPayload.getCountry().getCode());
        }
        return Optional.empty();
    }

    public static List<String> getAcceptedCountryCodes(AccountPayload<?> accountPayload) {
        if (accountPayload instanceof SepaAccountPayload sepaAccountPayload) {
            return new ArrayList<>(sepaAccountPayload.getAcceptedCountryCodes());
        } else if (accountPayload instanceof SepaInstantAccountPayload sepaInstantAccountPayload) {
            return new ArrayList<>(sepaInstantAccountPayload.getAcceptedCountryCodes());
        } else if (accountPayload instanceof CountryBasedAccountPayload countryBasedAccountPayload) {
            return Collections.singletonList(countryBasedAccountPayload.getCountry().getCode());
        }
        return List.of();
    }

    public static boolean showReasonForPayment(AccountPayload<?> accountPayload) {
        boolean isCrypto = accountPayload instanceof CryptoAssetAccountPayload;
        boolean isF2F = accountPayload instanceof F2FAccountPayload;
        boolean isCashByMail = accountPayload instanceof CashByMailAccountPayload;
        boolean isUserDefined = accountPayload instanceof UserDefinedFiatAccountPayload;
        return !isCrypto && !isF2F && !isCashByMail && !isUserDefined;
    }
}
