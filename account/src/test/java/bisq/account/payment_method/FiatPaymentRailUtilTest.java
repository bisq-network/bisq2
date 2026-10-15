package bisq.account.payment_method;

import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.locale.Country;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FiatPaymentRailUtilTest {

    @Test
    void sepaCountriesContainMontenegro() {
        assertThat(FiatPaymentRailUtil.getAllSepaCountryCodes()).contains("ME");
    }

    @Test
    void montenegroResolvesToCountryInSepaList() {
        assertThat(FiatPaymentRailUtil.getAllSepaCountries())
                .extracting(Country::getCode)
                .contains("ME");
    }

    @Test
    void montenegroIsSupportedBySepaAndSepaInstantRails() {
        assertThat(FiatPaymentRail.SEPA.getSupportedCountries())
                .extracting(Country::getCode)
                .contains("ME");
        assertThat(FiatPaymentRail.SEPA_INSTANT.getSupportedCountries())
                .extracting(Country::getCode)
                .contains("ME");
    }
}
