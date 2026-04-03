package bisq.api.dto.mappings.account;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.api.dto.account.AccountMetadataDto;
import bisq.mu_sig.MuSigTradeAmountLimits;
import bisq.presentation.formatters.DateFormatter;

public final class PaymentAccountMetadataDtoMapping {
    private PaymentAccountMetadataDtoMapping() {
    }

    public static AccountMetadataDto mapAccountMetadata(Account<? extends PaymentMethod<?>, ?> account) {
        String tradeLimitInfo = MuSigTradeAmountLimits.getFormattedMaxTradeLimit(account.getPaymentMethod().getPaymentRail());
        String creationDate = DateFormatter.formatDate(account.getCreationDate());
        String tradeDuration = account.getPaymentMethod().getPaymentRail().getTradeDuration().getDisplayString();
        return new AccountMetadataDto(creationDate, tradeLimitInfo, tradeDuration);
    }
}
