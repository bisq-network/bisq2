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

package bisq.api.rest_api.endpoints.config;

import bisq.api.dto.config.ApiCapabilitiesDto;
import bisq.api.dto.config.TradeAmountLimitsDto;
import bisq.api.rest_api.endpoints.trades.TradeRestApi;
import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigRestApiTest {

    @Test
    void getTradeAmountLimitsMirrorsCoreConstants() {
        ConfigRestApi restApi = new ConfigRestApi();

        Response response = restApi.getTradeAmountLimits();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        TradeAmountLimitsDto dto = (TradeAmountLimitsDto) response.getEntity();

        // The DTO must carry exactly what core exposes — this is the single-source-of-truth contract
        // the mobile client and node both rely on. If a core constant changes, this test fails loudly.
        assertThat(dto.defaultMinUsdTradeAmount().getValue())
                .isEqualTo(BisqEasyTradeAmountLimits.DEFAULT_MIN_USD_TRADE_AMOUNT.getValue());
        assertThat(dto.defaultMinUsdTradeAmount().getCode()).isEqualTo("USD");
        assertThat(dto.maxUsdTradeAmount().getValue())
                .isEqualTo(BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT.getValue());
        assertThat(dto.maxUsdTradeAmount().getCode()).isEqualTo("USD");
        assertThat(dto.tolerance()).isEqualTo(BisqEasyTradeAmountLimits.TOLERANCE);
        assertThat(dto.requiredReputationScorePerUsd())
                .isEqualTo(BisqEasyTradeAmountLimits.getRequiredReputationScorePerUsd());
    }

    @Test
    void getCapabilitiesListsSupportedFeaturesWithVersion() {
        ConfigRestApi restApi = new ConfigRestApi();

        Response response = restApi.getCapabilities();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        ApiCapabilitiesDto dto = (ApiCapabilitiesDto) response.getEntity();
        assertThat(dto.apiVersion()).isNotBlank();
        assertThat(dto.features()).containsExactlyElementsOf(ApiFeature.allKeys());
        assertThat(dto.features()).contains(ApiFeature.CLOSED_TRADES.getKey());
    }

    /**
     * Anti-drift guard: a declared feature must be backed by a real, wired endpoint/topic — never a
     * key for something not implemented in this build. As features are added here, extend this check.
     */
    @Test
    void everyDeclaredFeatureIsBackedByARealImplementation() {
        for (ApiFeature feature : ApiFeature.values()) {
            switch (feature) {
                case CLOSED_TRADES -> assertThat(hasEndpoint(TradeRestApi.class, "/closed"))
                        .as("closed-trades must expose GET /trades/closed")
                        .isTrue();
            }
        }
    }

    private static boolean hasEndpoint(Class<?> resource, String path) {
        return Arrays.stream(resource.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(GET.class) && isPath(m, path));
    }

    private static boolean isPath(Method method, String path) {
        Path annotation = method.getAnnotation(Path.class);
        return annotation != null && annotation.value().equals(path);
    }
}
