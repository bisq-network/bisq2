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
import bisq.api.access.permissions.PermissionService;
import bisq.api.dto.config.ApiCapabilitiesDto;
import bisq.api.dto.config.TradeAmountLimitsDto;
import bisq.api.rest_api.endpoints.trades.TradeRestApi;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.api.web_socket.domain.network.NetworkInfoWebSocketService;
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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

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
        assertThat(dto.features()).contains(ApiFeature.NETWORK_INFO.getKey());
    }

    /**
     * The keys are the wire contract capability-gated clients match on, so they must stay stable even
     * if the enum constants get renamed.
     */
    @Test
    void featureKeysAreStableWireIdentifiers() {
        assertThat(ApiFeature.CLOSED_TRADES.getKey()).isEqualTo("closed-trades");
        assertThat(ApiFeature.NETWORK_INFO.getKey()).isEqualTo("network-info");
    }

    /**
     * Anti-drift guard: a declared feature must be backed by a real, wired endpoint/topic — never a
     * key for something not implemented in this build.
     * <p>
     * A switch <i>expression</i> on purpose: exhaustiveness is compile-checked, so adding an
     * {@link ApiFeature} without extending this check breaks the build instead of being silently
     * skipped (arrow-form switch statements over enums are not exhaustiveness-checked, which is
     * how a feature could otherwise ship unguarded). Each case asserts its own specifics and
     * yields; the yielded value exists only to force the expression form.
     */
    @Test
    void everyDeclaredFeatureIsBackedByARealImplementation() {
        for (ApiFeature feature : ApiFeature.values()) {
            boolean checked = switch (feature) {
                case CLOSED_TRADES -> {
                    assertThat(hasEndpoint(TradeRestApi.class, "/closed"))
                            .as("closed-trades must expose GET /trades/closed")
                            .isTrue();
                    yield true;
                }
                case NETWORK_INFO -> {
                    BaseWebSocketService service = routeTopic(Topic.NETWORK_INFO);
                    assertThat(service)
                            .as("SubscriptionService must route NETWORK_INFO to a live NetworkInfoWebSocketService")
                            .isInstanceOf(NetworkInfoWebSocketService.class);
                    assertThat(topicOf(service))
                            .as("network-info must be backed by a service bound to Topic.NETWORK_INFO")
                            .isEqualTo(Topic.NETWORK_INFO);
                    yield true;
                }
            };
            assertThat(checked).isTrue();
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

    private static Topic topicOf(BaseWebSocketService service) {
        try {
            Field field = BaseWebSocketService.class.getDeclaredField("topic");
            field.setAccessible(true);
            return (Topic) field.get(service);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not read the topic of " + service.getClass().getSimpleName(), e);
        }
    }

    /**
     * Resolves the topic through a real {@link SubscriptionService} so the check covers the constructor
     * wiring and the routing switch, not just a field declaration.
     */
    @SuppressWarnings("unchecked")
    private static BaseWebSocketService routeTopic(Topic topic) {
        SubscriptionService subscriptionService = new SubscriptionService(
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                mock(AlertNotificationsService.class, RETURNS_DEEP_STUBS),
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(TradeService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BisqEasyService.class, RETURNS_DEEP_STUBS),
                mock(NetworkService.class, RETURNS_DEEP_STUBS),
                mock(OpenTradeItemsService.class, RETURNS_DEEP_STUBS),
                mock(PermissionService.class),
                false);
        try {
            Method method = SubscriptionService.class.getDeclaredMethod("findWebSocketService", Topic.class);
            method.setAccessible(true);
            Optional<BaseWebSocketService> routed =
                    (Optional<BaseWebSocketService>) method.invoke(subscriptionService, topic);
            return routed.orElseThrow(() -> new AssertionError("SubscriptionService does not route " + topic));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not resolve the WebSocketService for " + topic, e);
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
