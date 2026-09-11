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
import bisq.common.util.ExceptionUtil;
import bisq.network.NetworkService;
import bisq.network.http.HttpRequest;
import bisq.network.http.HttpRequestService;
import bisq.network.http.HttpRequestServiceConfig;
import bisq.network.http.HttpRequestUrlProvider;
import bisq.network.http.utils.HttpException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Slf4j
public class MobileNotificationRelayClient extends HttpRequestService<MobileNotificationRelayClient.RequestData, MobileNotificationRelayResult> {

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
    protected MobileNotificationRelayResult parseResult(String json) {
        // Only 2xx bodies reach this method; non-2xx surfaces as HttpException and is
        // translated in sendToRelayServer. Current relays answer 2xx with a structured
        // accepted result; older relays answer with a plain ack, mapped to accepted.
        // Logged at DEBUG: response bodies may include diagnostic data referencing
        // tokens or operator details — keep INFO output free of them.
        log.debug("Relay v1 response: {}", json);
        return MobileNotificationRelayResult.fromJson(json)
                .orElseGet(MobileNotificationRelayResult::accepted);
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
     * timeout, lifecycle). Completes normally with a rejected result when the
     * relay answered 400 with its structured body (the gateway's verdict, e.g.
     * an unregistered token); completes exceptionally for transient failures
     * (5xx, timeout, transport) and for rejections without a structured body.
     */
    public CompletableFuture<MobileNotificationRelayResult> sendToRelayServer(boolean isAndroid,
                                                                              String deviceToken,
                                                                              String encryptedBase64,
                                                                              boolean mutableContent) {
        return withRejectionTranslation(request(new RequestData(isAndroid, deviceToken, encryptedBase64, mutableContent)));
    }

    static CompletableFuture<MobileNotificationRelayResult> withRejectionTranslation(
            CompletableFuture<MobileNotificationRelayResult> request) {
        return request.handle((result, throwable) -> {
            if (throwable == null) {
                return result;
            }
            return structuredRejection(throwable)
                    .orElseThrow(() -> throwable instanceof CompletionException completionException
                            ? completionException
                            : new CompletionException(throwable));
        });
    }

    /**
     * Recovers the relay's structured verdict from a 400 response. Only 400 carries one —
     * the relay answers gateway rejections with 400 plus the serialized result, while its
     * 5xx responses have an empty body — so any other failure is transient by contract.
     */
    static Optional<MobileNotificationRelayResult> structuredRejection(Throwable throwable) {
        return ExceptionUtil.getRootCause(throwable) instanceof HttpException httpException
                && httpException.getResponseCode() == 400
                ? MobileNotificationRelayResult.fromJson(httpException.getMessage())
                : Optional.empty();
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
