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
import bisq.common.util.ExceptionUtil;
import bisq.i18n.Res;
import bisq.network.BaseService;
import bisq.network.NetworkService;
import bisq.network.http.BaseHttpClient;
import bisq.network.http.utils.HttpException;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;


@Slf4j
public class MobileNotificationRelayClient extends BaseService {
    private static final String SUCCESS = "success";
    private static final String ENDPOINT = "relay";

    private static final ExecutorService POOL = ExecutorFactory.newCachedThreadPool("MobileNotificationsService", 1, 5, 60);

    public MobileNotificationRelayClient(Config conf, NetworkService networkService) {
      super("", conf, networkService);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        return httpClient.map(BaseHttpClient::shutdown)
                .orElse(CompletableFuture.completedFuture(true));
    }

    public CompletableFuture<Boolean> sendToRelayServer(boolean isAndroid,
                                                        String deviceTokenHex,
                                                        String encryptedMessageHex) {
        try {
            return sendToRelayServer(isAndroid,
                    deviceTokenHex,
                    encryptedMessageHex,
                    new AtomicInteger(0))
                    .exceptionallyCompose(throwable -> {
                        if (throwable instanceof RetryException retryException) {
                            return sendToRelayServer(isAndroid,
                                    deviceTokenHex,
                                    encryptedMessageHex,
                                    retryException.getRecursionDepth());
                        } else if (ExceptionUtil.getRootCause(throwable) instanceof RetryException retryException) {
                            return sendToRelayServer(isAndroid,
                                    deviceTokenHex,
                                    encryptedMessageHex,
                                    retryException.getRecursionDepth());
                        } else {
                            return CompletableFuture.failedFuture(throwable);
                        }
                    });
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("Too many requests. Try again later."));
        }
    }

    public String getSelectedProviderBaseUrl() {
        return Optional.ofNullable(selectedProvider.get()).map(MobileNotificationRelayClient.Provider::getBaseUrl).orElse(Res.get("data.na"));
    }

    private CompletableFuture<Boolean> sendToRelayServer(boolean isAndroid,
                                                         String deviceTokenHex,
                                                         String encryptedMessageHex,
                                                         AtomicInteger recursionDepth) {
        if (noProviderAvailable) {
            return CompletableFuture.failedFuture(new RuntimeException("No mobile notification relay provider available"));
        }
        if (shutdownStarted) {
            return CompletableFuture.failedFuture(new RuntimeException("Shutdown has already started"));
        }

        try {
            return CompletableFuture.supplyAsync(() -> {
                        Provider provider = checkNotNull(selectedProvider.get(), "Selected provider must not be null.");
                        BaseHttpClient client = networkService.getHttpClient(provider.getBaseUrl(), userAgent, provider.getTransportType());
                        httpClient = Optional.of(client);
                        long ts = System.currentTimeMillis();
                        try {
                            String param = ENDPOINT + "?" +
                                    "isAndroid=" + isAndroid +
                                    "&token=" + deviceTokenHex +
                                    "&msg=" + encryptedMessageHex;

                            Pair<String, String> header = new Pair<>("User-Agent", userAgent);
                            String result = client.get(param, Optional.of(header));

                            log.info("Received response from {}/{} after {} ms", client.getBaseUrl(), ENDPOINT, System.currentTimeMillis() - ts);
                            selectedProvider.set(selectNextProvider());
                            shutdownHttpClient(client);
                            return result.equals(SUCCESS);
                        } catch (Exception e) {
                            shutdownHttpClient(client);
                            if (shutdownStarted) {
                                throw new RuntimeException("Shutdown has already started");
                            }

                            Throwable rootCause = ExceptionUtil.getRootCause(e);
                            log.warn("Encountered exception from provider {}", provider.getBaseUrl(), rootCause);

                            if (rootCause instanceof HttpException httpException) {
                                int responseCode = httpException.getResponseCode();
                                // If not server error we pass the error to the client
                                // 408 (Request Timeout) and 429 (Too Many Requests) are usually transient
                                // and should rotate to another provider.
                                if (responseCode < 500 && responseCode != 408 && responseCode != 429) {
                                    throw new CompletionException(e);
                                }
                            }

                            int numRecursions = recursionDepth.incrementAndGet();
                            if (numRecursions < numTotalCandidates && failedProviders.size() < numTotalCandidates) {
                                failedProviders.add(provider);
                                selectedProvider.set(selectNextProvider());
                                log.warn("We retry the request with new provider {}", selectedProvider.get().getBaseUrl());
                                throw new RetryException("Retrying with next provider", recursionDepth);
                            } else {
                                log.warn("We exhausted all possible providers and give up");
                                throw new RuntimeException("We failed at all possible providers and give up");
                            }
                        }
                    }, POOL)
                    .completeOnTimeout(null, conf.getTimeoutInSeconds(), SECONDS)
                    .thenCompose(result -> {
                        if (result == null) {
                            // Timeout occurred - add provider to failed list before retrying
                            Provider currentProvider = selectedProvider.get();
                            if (currentProvider != null) {
                                failedProviders.add(currentProvider);
                                log.warn("Request to provider {} timed out after {} seconds",
                                        currentProvider.getBaseUrl(), conf.getTimeoutInSeconds());
                            }
                            return CompletableFuture.failedFuture(new RetryException("Timeout", recursionDepth));
                        }
                        return CompletableFuture.completedFuture(result);
                    });
        } catch (RejectedExecutionException e) {
            log.error("Executor rejected task.", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void shutdownHttpClient(BaseHttpClient client) {
        try {
            client.shutdown();
        } catch (Exception ignore) {
        }
    }
}
