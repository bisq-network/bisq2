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

package bisq.api.rest_api.endpoints.payment_accounts;

import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodDto;
import bisq.i18n.Res;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for the mobile-client-facing GET /payment-accounts/payment-methods/fiat endpoint.
 * <p>
 * Root cause of the original bug: this endpoint iterated ALL {@code FiatPaymentRail} values (including
 * TELE_BIRR, a Bisq Easy-only rail added without a corresponding {@code FiatPaymentRailDto} constant) and
 * hand-filtered only CUSTOM and CASH_APP. Mapping TELE_BIRR through
 * {@code FiatPaymentRailDtoMapping.fromBisq2Model} then called
 * {@code FiatPaymentRailDto.valueOf("TELE_BIRR")}, which threw {@link IllegalArgumentException} since no
 * such DTO constant exists — caught by the endpoint's generic catch block and surfaced as an HTTP 500 to
 * every caller, not just Ethiopian-market ones.
 */
class PaymentAccountsRestApiTest {

    @BeforeAll
    static void initBundles() {
        // FiatPaymentMethodDtoMapping resolves trade-duration display strings via Res.get(), which
        // requires the resource bundles to be loaded first.
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    void getFiatPaymentMethodsDoesNotThrowForTeleBirr() {
        PaymentAccountsRestApi restApi = new PaymentAccountsRestApi(null);

        Response response = restApi.getFiatPaymentMethods();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    void getFiatPaymentMethodsExcludesAccountlessOnlyRails() {
        PaymentAccountsRestApi restApi = new PaymentAccountsRestApi(null);

        Response response = restApi.getFiatPaymentMethods();

        @SuppressWarnings("unchecked")
        List<FiatPaymentMethodDto> items = (List<FiatPaymentMethodDto>) response.getEntity();
        assertThat(items).isNotEmpty();
        // TELE_BIRR has no FiatPaymentRailDto constant at all, so if it ever leaked into this list the
        // stream building `items` above would already have thrown before we got here. This assertion
        // documents the intent for future readers.
        assertThat(items)
                .extracting(FiatPaymentMethodDto::paymentRail)
                .extracting(Enum::name)
                .doesNotContain("TELE_BIRR", "CASH_APP", "CUSTOM");
    }
}
