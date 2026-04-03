package bisq.account.payment_method;

import bisq.i18n.Res;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TradeDurationTest {

    @Test
    void displayStringUsesActiveLanguageTag() {
        Res.setAndApplyLanguageTag("en");
        String english = TradeDuration.DAYS_4.getDisplayString();

        Res.setAndApplyLanguageTag("es");
        String spanish = TradeDuration.DAYS_4.getDisplayString();

        assertNotEquals(english, spanish);
    }
}
