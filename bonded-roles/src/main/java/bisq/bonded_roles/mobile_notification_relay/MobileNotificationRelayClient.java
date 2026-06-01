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

package bisq.bonded_roles.mobile_notification_relay;

import bisq.common.data.Pair;
import bisq.common.json.JsonMapperProvider;
import bisq.common.threading.ExecutorFactory;
import bisq.network.NetworkService;
import bisq.network.http.HttpRequest;
import bisq.network.http.HttpRequestService;
import bisq.network.http.HttpRequestServiceConfig;
import bisq.network.http.HttpRequestUrlProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class MobileNotificationRelayClient extends HttpRequestService<MobileNotificationRelayClient.RequestData, Boolean> {

    private static ExecutorService getExecutorService() {
        return ExecutorFactory.newCachedThreadPool(MobileNotificationRelayClient.class.getSimpleName(),
                1,
                5,
                60);
    }

    public MobileNotificationRelayClient(HttpRequestServiceConfig conf, NetworkService networkService) {
        super(conf,
                networkService,
                getExecutorService());
    }

    @Override
    protected Boolean parseResult(String json) {
        // The relay v1 endpoint signals success with a 2xx response — anything
        // non-2xx surfaces as HttpException upstream, never reaches this method.
        // TODO: if the relay starts returning structured JSON (e.g.
        // {"success": false, "reason": "..."} with a 200), parse it here so we
        // don't silently report success on a logical failure.
        // Logged at DEBUG: relay responses are small acks today, but future
        // relay versions may include diagnostic data referencing tokens or
        // operator details — keep INFO output free of raw response bodies.
        log.debug("Relay v1 response: {}", json);
        return true;
    }

    @Override
    protected HttpRequest buildRequest(HttpRequestUrlProvider provider, RequestData requestData) {
        // POST /v1/{platform}/device/{token} with JSON body containing the
        // Base64-encoded encrypted payload. Avoids the hex-encoding round-trip
        // of the legacy GET /relay endpoint which corrupts binary ciphertext.
        String platformPath = requestData.isAndroid() ? "/v1/fcm/device/" : "/v1/apns/device/";
        String path = platformPath + requestData.deviceToken();
        // Device tokens are an installation identifier — redact from logs.
        String logPath = platformPath + "<redacted>";
        String body = buildJsonBody(requestData.encryptedMessage(), true, requestData.mutableContent());
        // Push delivery is best-effort and 5xx from FCM/APNS is typically
        // transient. We accept at-least-once semantics: a rare duplicate
        // banner is preferable to a silent drop. Note: neither FCM
        // collapse_key nor APNS apns-collapse-id / apns-id dedupes
        // already-delivered notifications — they only collapse while the
        // device is offline.
        return HttpRequest.post(path,
                logPath,
                body,
                new Pair<>("Content-Type", "application/json"),
                true);
    }

    /**
     * Sends a push notification via the relay's v1 POST endpoint, using the
     * full {@link HttpRequestService} pipeline (provider failover, retry,
     * timeout, lifecycle).
     */
    public CompletableFuture<Boolean> sendToRelayServer(boolean isAndroid,
                                                        String deviceToken,
                                                        String encryptedBase64,
                                                        boolean mutableContent) {
        return request(new RequestData(isAndroid, deviceToken, encryptedBase64, mutableContent));
    }

    static String buildJsonBody(String encryptedBase64, boolean isUrgent, boolean isMutableContent) {
        try {
            return JsonMapperProvider.get().writeValueAsString(Map.of(
                    "encrypted", encryptedBase64,
                    "isUrgent", isUrgent,
                    "isMutableContent", isMutableContent));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize push notification body", e);
        }
    }

    public record RequestData(boolean isAndroid,
                              String deviceToken,
                              String encryptedMessage,
                              boolean mutableContent) {
    }
}
