package bisq.api.dto.mappings.account;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.api.dto.account.AccountMetadataDto;
import bisq.presentation.formatters.DateFormatter;

public final class PaymentAccountMetadataDtoMapping {
    private PaymentAccountMetadataDtoMapping() {
    }

    public static AccountMetadataDto mapAccountMetadata(Account<? extends PaymentMethod<?>, ?> account) {
        String creationDate = DateFormatter.formatDate(account.getCreationDate());
        PaymentRail paymentRail = account.getPaymentMethod().getPaymentRail();
        String tradeLimitInfo = PaymentMethodDtoHelper.getTradeLimitInfo(paymentRail);
        String tradeDuration = PaymentMethodDtoHelper.getTradeDuration(paymentRail);
        return new AccountMetadataDto(creationDate, tradeLimitInfo, tradeDuration);
    }
}
