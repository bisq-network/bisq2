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

package bisq.network.tor.controller;

import bisq.network.tor.common.util.StringUtils;
import bisq.network.tor.controller.events.events.EventType;
import bisq.network.tor.controller.events.events.HsDescEvent;
import bisq.network.tor.controller.events.listener.FilteredHsDescEventListener;
import bisq.network.tor.controller.exceptions.HsDescUploadFailedException;
import bisq.network.tor.onionservice.OnionServiceKey;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PublishOnionAddressService extends FilteredHsDescEventListener {
    private final TorControlProtocol torControlProtocol;
    private final int timeout;
    private final OnionServiceKey onionServiceKey;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    @Getter
    private Optional<CompletableFuture<Void>> future = Optional.empty();

    public PublishOnionAddressService(TorControlProtocol torControlProtocol, int timeout, OnionServiceKey onionServiceKey) {
        super(EventType.HS_DESC, onionServiceKey.getOnionAddress(), Set.of(HsDescEvent.Action.UPLOAD));

        this.torControlProtocol = torControlProtocol;
        this.timeout = timeout;
        this.onionServiceKey = onionServiceKey;
    }

    public CompletableFuture<Void> publish(int onionServicePort, int localPort) {
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("PublishOnionAddressService")
                .setDaemon(true)
                .setPriority(Thread.NORM_PRIORITY)
                .build());
        future = Optional.of(CompletableFuture.runAsync(() -> {
                    torControlProtocol.addHsDescEventListener(this);

                    torControlProtocol.addOnion(onionServiceKey, onionServicePort, localPort);

                    boolean isSuccess;
                    try {
                        isSuccess = countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore interrupted state
                        throw new HsDescUploadFailedException(e);
                    } catch (Exception e) {
                        throw new HsDescUploadFailedException(e);
                    }
                    if (!isSuccess) {
                        throw new HsDescUploadFailedException("Could not get onion address upload completed in " + timeout + " ms");
                    }
                }, executor)
                .whenComplete((nil, throwable) -> {
                            torControlProtocol.removeHsDescEventListener(this);
                            executor.shutdown();
                        }
                ));
        return future.get();
    }

    public void shutdown() {
        torControlProtocol.removeHsDescEventListener(this);
        if (countDownLatch.getCount() > 0) {
            countDownLatch.countDown();
        }
    }

    @Override
    public void onFilteredEvent(HsDescEvent hsDescEvent) {
        log.info("Received HsDescEvent: {}", StringUtils.truncate(hsDescEvent, 200));
        if (countDownLatch.getCount() > 0) {
            log.info("Publishing of onion address completed. Event {}", StringUtils.truncate(hsDescEvent, 200));
        }
        countDownLatch.countDown();
    }
}
