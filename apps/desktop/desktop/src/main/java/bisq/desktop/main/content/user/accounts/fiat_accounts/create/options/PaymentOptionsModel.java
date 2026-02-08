package bisq.desktop.main.content.user.accounts.fiat_accounts.create.options;

import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.view.Model;
import lombok.Getter;
import lombok.Setter;

/**
 * STUB IMPLEMENTATION - Options step model
 */

@Getter
public class PaymentOptionsModel implements Model {
    @Setter
    private PaymentMethod<?> paymentMethod;
}
