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
import bisq.common.threading.ExecutorFactory;
import bisq.network.HttpRequestBaseService;
import bisq.network.NetworkService;
import bisq.network.http.BaseHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;



@Slf4j
public class MobileNotificationRelayClient extends HttpRequestBaseService {
    private static final String SUCCESS = "success";
    private static final String ENDPOINT = "relay";

    private static final ExecutorService POOL = ExecutorFactory.newCachedThreadPool("MobileNotificationsService", 1, 5, 60);

    public MobileNotificationRelayClient(Config conf, NetworkService networkService) {
      super("", conf, networkService, POOL);
    }

    public CompletableFuture<Boolean> sendToRelayServer(boolean isAndroid,
                                                        String deviceTokenHex,
                                                        String encryptedMessageHex) {
        return executeWithRetry(() -> makeRequest(provider -> {
            long ts = System.currentTimeMillis();
            String param = ENDPOINT + "?" +
                    "isAndroid=" + isAndroid +
                    "&token=" + deviceTokenHex +
                    "&msg=" + encryptedMessageHex;
            
            BaseHttpClient client = networkService.getHttpClient(provider.getBaseUrl(), userAgent, provider.getTransportType());
            try {
                String result = client.get(param, Optional.of(new Pair<>("User-Agent", userAgent)));

                log.info("Received response from {}/{} after {} ms", provider.getBaseUrl(), ENDPOINT, System.currentTimeMillis() - ts);
                return CompletableFuture.completedFuture(result.equals(SUCCESS));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
