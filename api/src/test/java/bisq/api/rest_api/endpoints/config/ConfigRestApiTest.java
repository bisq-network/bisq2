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

import bisq.api.access.AllowUnauthenticated;
import bisq.api.dto.config.ApiCapabilitiesDto;
import bisq.api.dto.config.TradeAmountLimitsDto;
import bisq.api.rest_api.endpoints.trades.TradeRestApi;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.api.web_socket.domain.network.NetworkInfoWebSocketService;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionService;
import bisq.api.web_socket.subscription.Topic;
import bisq.bisq_easy.BisqEasyService;
import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.chat.ChatService;
import bisq.network.NetworkService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
     * key for something not implemented in this build.
     * <p>
     * Implemented as a switch <i>expression</i> on purpose: exhaustiveness is compile-checked, so
     * adding an {@link ApiFeature} without extending this check breaks the build instead of being
     * silently skipped (which is how NETWORK_INFO originally slipped past the old switch statement).
     */
    @Test
    void everyDeclaredFeatureIsBackedByARealImplementation() {
        for (ApiFeature feature : ApiFeature.values()) {
            boolean backed =
                    switch (feature) {
                        case CLOSED_TRADES -> hasEndpoint(TradeRestApi.class, "/closed");
                        case NETWORK_INFO -> networkInfoSubscriptionIsServed();
                    };
            assertThat(backed)
                    .as("feature '%s' is declared in /config/capabilities but not backed by a real implementation", feature.getKey())
                    .isTrue();
        }
    }

    /**
     * Proves {@link Topic#NETWORK_INFO} is served through the real subscription path — a
     * {@link bisq.api.web_socket.subscription.SubscriptionService} built the production way routes a
     * NETWORK_INFO subscription request to its wired {@link NetworkInfoWebSocketService} and the
     * subscriber ends up registered. Deleting the topic, the service, or its registration in
     * SubscriptionService makes this fail (or not compile), which is the point of the guard.
     */
    private static boolean networkInfoSubscriptionIsServed() {
        try {
            SubscriptionService subscriptionService = new SubscriptionService(
                    mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                    mock(AlertNotificationsService.class, RETURNS_DEEP_STUBS),
                    mock(ChatService.class, RETURNS_DEEP_STUBS),
                    mock(TradeService.class, RETURNS_DEEP_STUBS),
                    mock(UserService.class, RETURNS_DEEP_STUBS),
                    mock(BisqEasyService.class, RETURNS_DEEP_STUBS),
                    mock(NetworkService.class, RETURNS_DEEP_STUBS),
                    mock(OpenTradeItemsService.class, RETURNS_DEEP_STUBS));

            WebSocket webSocket = mock(WebSocket.class);
            when(webSocket.send(anyString())).thenReturn(ReadyFutureImpl.create(null));

            String requestJson =
                    "{\"type\":\"SubscriptionRequest\",\"requestId\":\"config-guard-1\",\"topic\":\"NETWORK_INFO\"}";
            subscriptionService.onMessage(requestJson, webSocket);

            Field field = SubscriptionService.class.getDeclaredField("subscriberRepository");
            field.setAccessible(true);
            SubscriberRepository subscriberRepository = (SubscriberRepository) field.get(subscriptionService);
            return !subscriberRepository.findSubscribers(Topic.NETWORK_INFO).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Guards public reachability: config is version-global, non-sensitive metadata and must be
     * readable by any client. Without {@link AllowUnauthenticated} the authorization filter returns
     * 403 for /config whenever authorizationRequired=true (RestPermissionMapping has no /config rule).
     */
    @Test
    void configIsPublicSoTheAuthorizationFilterDoesNotReject403() {
        assertThat(ConfigRestApi.class.isAnnotationPresent(AllowUnauthenticated.class))
                .as("/config must be @AllowUnauthenticated — it has no RestPermissionMapping rule and would otherwise 403")
                .isTrue();
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
