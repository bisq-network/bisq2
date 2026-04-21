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
import bisq.network.http.BaseHttpClient;
import bisq.network.http.HttpRequestService;
import bisq.network.http.HttpRequestServiceConfig;
import bisq.network.http.HttpRequestUrlProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Slf4j
public class MobileNotificationRelayClient extends HttpRequestService<MobileNotificationRelayClient.RequestData, String> {

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
    protected String parseResult(String json) {
        return json;
    }

    @Override
    protected String getParam(HttpRequestUrlProvider provider, RequestData requestData) {
        String params = provider.getApiPath() + "?" +
                "isAndroid=" + requestData.isAndroid() +
                "&token=" + requestData.getDeviceToken() +
                "&msg=" + requestData.getEncryptedMessage();
        if (requestData.isMutableContent()) {
            params += "&mutableContent=true";
        }
        return params;
    }

    /**
     * Sends a push notification via the relay's v1 POST endpoint.
     * Uses POST /v1/apns/device/{token} or /v1/fcm/device/{token} with a JSON body
     * containing the Base64-encoded encrypted payload. This avoids the hex encoding
     * round-trip of the legacy GET /relay endpoint which corrupts binary ciphertext.
     */
    public CompletableFuture<Boolean> sendToRelayServer(boolean isAndroid,
                                                        String deviceToken,
                                                        String encryptedBase64,
                                                        boolean mutableContent) {
        if (noProviderAvailable) {
            return CompletableFuture.failedFuture(new RuntimeException("No relay provider available"));
        }

        HttpRequestUrlProvider provider = selectedProvider.get();
        if (provider == null) {
            return CompletableFuture.failedFuture(new RuntimeException("No relay provider selected"));
        }

        String platformPath = isAndroid ? "/v1/fcm/device/" : "/v1/apns/device/";
        String baseUrl = provider.getBaseUrl().replaceAll("/+$", "");
        String fullUrl = baseUrl + platformPath + deviceToken;
        String jsonBody = buildJsonBody(encryptedBase64, true, mutableContent);

        return CompletableFuture.supplyAsync(() -> {
            BaseHttpClient client = networkService.getHttpClient(
                    fullUrl, userAgent, provider.getTransportType());
            try {
                log.info("Sending push notification via POST to {}", provider.getBaseUrl() + platformPath + "...");
                String response = client.post(jsonBody,
                        Optional.of(new Pair<>("Content-Type", "application/json")));
                log.info("Relay v1 response: {}", response);
                return true;
            } catch (Exception e) {
                throw new CompletionException(e);
            } finally {
                try {
                    client.shutdown();
                } catch (Exception e) {
                    log.warn("Failed to shut down HTTP client", e);
                }
            }
        }, executorService);
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

    @Getter
    public static class RequestData {
        private final boolean isAndroid;
        private final String deviceToken;
        private final String encryptedMessage;
        private final boolean mutableContent;

        public RequestData(boolean isAndroid, String deviceToken, String encryptedMessage, boolean mutableContent) {
            this.isAndroid = isAndroid;
            this.deviceToken = deviceToken;
            this.encryptedMessage = encryptedMessage;
            this.mutableContent = mutableContent;
        }
    }

}
