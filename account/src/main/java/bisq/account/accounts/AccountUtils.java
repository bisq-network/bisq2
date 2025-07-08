package bisq.account.accounts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AccountUtils {
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
            Optional.of(new ArrayList<>(sepaAccountPayload.getAcceptedCountryCodes()));
        } else if (accountPayload instanceof SepaInstantAccountPayload sepaInstantAccountPayload) {
            Optional.of(new ArrayList<>(sepaInstantAccountPayload.getAcceptedCountryCodes()));
        } else if (accountPayload instanceof CountryBasedAccountPayload countryBasedAccountPayload) {
            return Collections.singletonList(countryBasedAccountPayload.getCountry().getCode());
        }
        return List.of();
    }
}
