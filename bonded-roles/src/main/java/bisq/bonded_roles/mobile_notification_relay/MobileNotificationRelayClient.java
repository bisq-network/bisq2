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

import bisq.common.threading.ExecutorFactory;
import bisq.network.NetworkService;
import bisq.network.http.utils.HttpRequestService;
import bisq.network.http.utils.HttpRequestServiceConfig;
import bisq.network.http.utils.HttpRequestUrlProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

// TODO extract super class
// TODO WIP

@Slf4j
public class MobileNotificationRelayClient extends HttpRequestService<MobileNotificationRelayClient.RequestData, String> {
    private static final String SUCCESS = "success";
    private static final String ENDPOINT = "relay";

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
        return ENDPOINT + "?" +
                "isAndroid=" + requestData.isAndroid() +
                "&token=" + requestData.getDeviceTokenHex() +
                "&msg=" + requestData.getEncryptedMessageHex();
    }

    public CompletableFuture<Boolean> sendToRelayServer(boolean isAndroid,
                                                        String deviceTokenHex,
                                                        String encryptedMessageHex) {
        RequestData requestData = new RequestData(isAndroid,
                deviceTokenHex,
                encryptedMessageHex);
        return request(requestData)
                .thenApply(result -> result.equals(SUCCESS));
    }

    @Getter
    public static class RequestData {
        private final boolean isAndroid;
        private final String deviceTokenHex;
        private final String encryptedMessageHex;

        public RequestData(boolean isAndroid, String deviceTokenHex, String encryptedMessageHex) {
            this.isAndroid = isAndroid;
            this.deviceTokenHex = deviceTokenHex;
            this.encryptedMessageHex = encryptedMessageHex;
        }
    }

}
