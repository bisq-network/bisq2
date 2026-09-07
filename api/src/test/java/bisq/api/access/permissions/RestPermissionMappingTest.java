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

package bisq.api.access.permissions;

import bisq.api.ApiConfig;
import bisq.api.access.AllowUnauthenticated;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.rest_api.RestApiResourceConfig;
import bisq.api.rest_api.endpoints.access.AccessApi;
import bisq.api.rest_api.endpoints.alert_notifications.AlertNotificationsRestApi;
import bisq.api.rest_api.endpoints.chat.private_chat.PrivateChatRestApi;
import bisq.api.rest_api.endpoints.chat.public_chat.PublicChatRestApi;
import bisq.api.rest_api.endpoints.chat.trade.TradeChatMessagesRestApi;
import bisq.api.rest_api.endpoints.config.ConfigRestApi;
import bisq.api.rest_api.endpoints.contacts.ContactsRestApi;
import bisq.api.rest_api.endpoints.devices.DevicesRestApi;
import bisq.api.rest_api.endpoints.explorer.ExplorerRestApi;
import bisq.api.rest_api.endpoints.market_price.MarketPriceRestApi;
import bisq.api.rest_api.endpoints.offers.OfferbookRestApi;
import bisq.api.rest_api.endpoints.payment_accounts.PaymentAccountsRestApi;
import bisq.api.rest_api.endpoints.payment_accounts.UserDefinedPaymentAccountsRestApi;
import bisq.api.rest_api.endpoints.reputation.ReputationRestApi;
import bisq.api.rest_api.endpoints.settings.SettingsRestApi;
import bisq.api.rest_api.endpoints.trade_restricting_alert.TradeRestrictingAlertRestApi;
import bisq.api.rest_api.endpoints.trades.TradeRestApi;
import bisq.api.rest_api.endpoints.user_identity.UserIdentityRestApi;
import bisq.api.rest_api.endpoints.user_profile.UserProfileRestApi;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pins the table itself, as {@code SubscriptionPermissionMappingTest} does for the topics. The two are
 * meant to agree — that test's expectations are, in its own words, "the permission that already guards
 * the same data over REST" — and until now only the topic side of that mirror was asserted.
 * <p>
 * What makes a wrong row here worse than a wrong row there: {@link RestPermissionMapping} is
 * fail-closed, so a rule that is deleted, mistyped, or left behind by a renamed {@code @Path} does not
 * widen access, it removes it. Every request to that resource is refused with a bare 403, for every
 * client, including one holding grantAll. That is a feature outage with no error text to explain it,
 * and before this class the only rule any test touched was OFFERBOOK, by way of the single hardcoded
 * URI in {@code RestApiAuthorizationFilterTest}.
 */
class RestPermissionMappingTest {
    /**
     * Hand-written rather than read off the rules, which would assert the mapping against itself. The
     * paths are the {@code @Path} of the resource that serves them.
     */
    private static final Map<String, Permission> EXPECTED = Map.ofEntries(
            Map.entry("/trade-chat-channels", Permission.TRADE_CHAT_CHANNELS),
            Map.entry("/private-chat-channels", Permission.PRIVATE_CHAT_CHANNELS),
            Map.entry("/public-chat-channels", Permission.PUBLIC_CHAT_CHANNELS),
            Map.entry("/contacts", Permission.CONTACTS),
            Map.entry("/explorer", Permission.EXPLORER),
            Map.entry("/market-price", Permission.MARKET_PRICE),
            Map.entry("/offerbook", Permission.OFFERBOOK),
            Map.entry("/payment-accounts", Permission.PAYMENT_ACCOUNTS),
            Map.entry("/reputation", Permission.REPUTATION),
            Map.entry("/trade-restricting-alert", Permission.SETTINGS),
            Map.entry("/alert-notifications", Permission.SETTINGS),
            Map.entry("/settings", Permission.SETTINGS),
            Map.entry("/trades", Permission.TRADES),
            Map.entry("/user-identities", Permission.USER_IDENTITIES),
            Map.entry("/user-profiles", Permission.USER_PROFILES),
            Map.entry("/mobile-devices", Permission.MOBILE_DEVICES));

    /**
     * Reachable without a permission, so the mapping is never asked and no rule is expected.
     * {@code AccessApi} and {@code ConfigRestApi} say so with {@link AllowUnauthenticated} and are
     * skipped by the enumeration below on that basis; {@code openapi.json} does not, and is listed
     * here instead — which means that while {@code authorizationRequired} is on it answers a bare 403
     * to every client. Recorded so the exception is a decision someone can revisit rather than an
     * oversight nothing reports.
     */
    private static final Set<String> NO_RULE_ON_PURPOSE = Set.of("/openapi.json");

    private final RestPermissionMapping mapping = new RestPermissionMapping();

    @Test
    void everyPathRequiresThePermissionThatGuardsItsData() {
        EXPECTED.forEach((path, permission) -> assertThat(mapping.getRequiredPermission(path, "GET"))
                .as("%s", path)
                .isEqualTo(permission));
    }

    /** The rules end in {@code (/.*)?}, so a sub-resource is guarded by the same permission as its root. */
    @Test
    void aSubPathRequiresTheSamePermissionAsItsRoot() {
        EXPECTED.forEach((path, permission) -> assertThat(mapping.getRequiredPermission(path + "/nested/leaf", "POST"))
                .as("%s", path)
                .isEqualTo(permission));
    }

    /** The rules are written against the path the client asks for minus the application prefix. */
    @Test
    void theApplicationPrefixIsStrippedBeforeMatching() {
        assertThat(mapping.getRequiredPermission("/api/v1/public-chat-channels/discussion.bisq/messages", "GET"))
                .isEqualTo(Permission.PUBLIC_CHAT_CHANNELS);
    }

    /**
     * Fail-closed. Asserted so the day someone adds a catch-all rule to stop the 403s below, this says
     * what that costs: every path nobody thought about becomes reachable with whatever it grants.
     */
    @Test
    void aPathWithNoRuleIsRefusedRatherThanDefaulted() {
        assertThatThrownBy(() -> mapping.getRequiredPermission("/no-such-resource", "GET"))
                .isInstanceOf(ForbiddenException.class);
    }

    /**
     * The table above cannot notice a resource added after it, which is exactly how the gap it closes
     * came about. This walks what the node actually serves instead: every resource method registered in
     * {@link RestApiResourceConfig} that the authorization filter does not skip needs a rule, or the
     * request is refused.
     * <p>
     * The skip condition is the filter's own — {@code RestApiFilter#isAllowUnauthenticated}, which reads
     * the annotation off the resource method or its class — so the two cannot drift apart.
     */
    @Test
    void everyResourceMethodTheFilterGuardsHasARule() {
        List<String> authorized = authorizedResourcePaths();
        // Without this the loop below passes on an empty list, which is what an enumeration that
        // stopped finding resources would hand it.
        assertThat(authorized)
                .as("the enumeration found no resource methods, so the assertion below proves nothing")
                .contains("/public-chat-channels/{channelId}/messages");

        List<String> unmapped = new ArrayList<>();
        for (String path : authorized) {
            if (NO_RULE_ON_PURPOSE.contains(path)) {
                continue;
            }
            try {
                mapping.getRequiredPermission(path, "GET");
            } catch (ForbiddenException e) {
                unmapped.add(path);
            }
        }

        assertThat(unmapped)
                .as("these paths are served and authorized, but no rule answers for them, so every "
                        + "request to them is refused with a bare 403")
                .isEmpty();
    }

    /** Every path the authorization filter would consult the mapping for, one per resource method. */
    private static List<String> authorizedResourcePaths() {
        List<String> paths = new ArrayList<>();
        for (Class<?> resource : resourceConfig().getClasses()) {
            Path classPath = resource.getAnnotation(Path.class);
            if (classPath == null || resource.isAnnotationPresent(AllowUnauthenticated.class)) {
                continue;
            }
            for (Method method : resource.getDeclaredMethods()) {
                if (isResourceMethod(method) && !method.isAnnotationPresent(AllowUnauthenticated.class)) {
                    paths.add(fullPath(classPath, method));
                }
            }
        }
        return paths;
    }

    /** A resource method is one carrying an annotation that is itself a {@link HttpMethod}, e.g. {@code @GET}. */
    private static boolean isResourceMethod(Method method) {
        return Arrays.stream(method.getAnnotations())
                .map(Annotation::annotationType)
                .anyMatch(type -> type.isAnnotationPresent(HttpMethod.class));
    }

    private static String fullPath(Path classPath, Method method) {
        String methodPath = Optional.ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse("");
        return withLeadingSlash(classPath.value()) + (methodPath.isEmpty() ? "" : withLeadingSlash(methodPath));
    }

    private static String withLeadingSlash(String value) {
        return value.startsWith("/") ? value : "/" + value;
    }

    /**
     * The real config, so a resource registered without a rule is caught. Mocks throughout: nothing here
     * is called, the constructor only registers classes, and the bindings it declares are resolved by
     * HK2 at request time, which never happens in this test.
     */
    private static RestApiResourceConfig resourceConfig() {
        return new RestApiResourceConfig(mock(ApiConfig.class),
                mock(PermissionService.class),
                mock(SessionAuthenticationService.class),
                mock(AccessApi.class),
                mock(OfferbookRestApi.class),
                mock(TradeRestApi.class),
                mock(TradeChatMessagesRestApi.class),
                mock(PrivateChatRestApi.class),
                mock(PublicChatRestApi.class),
                mock(UserIdentityRestApi.class),
                mock(MarketPriceRestApi.class),
                mock(SettingsRestApi.class),
                mock(AlertNotificationsRestApi.class),
                mock(TradeRestrictingAlertRestApi.class),
                mock(ExplorerRestApi.class),
                mock(PaymentAccountsRestApi.class),
                mock(UserDefinedPaymentAccountsRestApi.class),
                mock(ReputationRestApi.class),
                mock(UserProfileRestApi.class),
                mock(DevicesRestApi.class),
                mock(ConfigRestApi.class),
                mock(ContactsRestApi.class));
    }
}
