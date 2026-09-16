package bisq.api.dto.mappings.account;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.account.timestamp.KeyType;
import bisq.presentation.formatters.DateFormatter;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public final class PaymentAccountDtoMappingHelper {
    private PaymentAccountDtoMappingHelper() {
    }

    public static KeyPair createDefaultKeyPair() {
        return KeyGeneration.generateDefaultEcKeyPair();
    }

    public static KeyType getDefaultKeyType() {
        return KeyType.EC;
    }

    public static String getCreationDate(Account<? extends PaymentMethod<?>, ?> account) {
        return DateFormatter.formatDate(account.getCreationDate());
    }

    public static String getTradeLimitInfo(Account<? extends PaymentMethod<?>, ?> account) {
        return PaymentMethodDtoMappingHelper.getTradeLimitInfo(getPaymentRail(account));
    }

    public static String getTradeDuration(Account<? extends PaymentMethod<?>, ?> account) {
        return PaymentMethodDtoMappingHelper.getTradeDuration(getPaymentRail(account));
    }

    private static PaymentRail getPaymentRail(Account<? extends PaymentMethod<?>, ?> account) {
        return account.getPaymentMethod().getPaymentRail();
    }
}
