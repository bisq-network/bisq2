package bisq.account.accounts;

public class AccountUtils {
    public static boolean isStateRequired(String countryCode) {
        return switch (countryCode) {
            case "US", "CA", "AU", "MY", "MX", "CN" -> true;
            default -> false;
        };
    }
}
