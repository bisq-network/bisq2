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

package bisq.tor.controller;

import bisq.common.threading.ExecutorFactory;
import bisq.common.util.StringUtils;
import bisq.security.keys.TorKeyPair;
import bisq.tor.controller.events.events.EventType;
import bisq.tor.controller.events.events.HsDescEvent;
import bisq.tor.controller.events.listener.FilteredHsDescEventListener;
import bisq.tor.controller.exceptions.HsDescUploadFailedException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PublishOnionAddressService extends FilteredHsDescEventListener {
    private final TorControlProtocol torControlProtocol;
    private final int timeout;
    private final TorKeyPair torKeyPair;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    @Getter
    private Optional<CompletableFuture<Void>> future = Optional.empty();

    public PublishOnionAddressService(TorControlProtocol torControlProtocol, int timeout, TorKeyPair torKeyPair) {
        super(EventType.HS_DESC, torKeyPair.getOnionAddress(), Set.of(HsDescEvent.Action.UPLOAD));

        this.torControlProtocol = torControlProtocol;
        this.timeout = timeout;
        this.torKeyPair = torKeyPair;
    }

    public CompletableFuture<Void> publish(int onionServicePort, int localPort) {
        future = Optional.of(CompletableFuture.runAsync(() -> {
                    torControlProtocol.addHsDescEventListener(this);

                    torControlProtocol.addOnion(torKeyPair, onionServicePort, localPort);

                    boolean isSuccess;
                    try {
                        isSuccess = countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        throw new HsDescUploadFailedException(e);
                    }
                    if (!isSuccess) {
                        throw new HsDescUploadFailedException("Could not get onion address upload completed in " + timeout / 1000 + " seconds");
                    }
                }, ExecutorFactory.newSingleThreadExecutor("PublishOnionAddressService"))
                .whenComplete((nil, throwable) ->
                        torControlProtocol.removeHsDescEventListener(this)));
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
