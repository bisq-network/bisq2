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

import bisq.tor.controller.events.events.EventType;
import bisq.tor.controller.events.events.HsDescEvent;
import bisq.tor.controller.events.events.HsDescFailedEvent;
import bisq.tor.controller.events.listener.FilteredHsDescEventListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OnionServiceOnlineStateService extends FilteredHsDescEventListener {
    private final TorControlProtocol torControlProtocol;
    private final String onionAddress;
    private final long timeout;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private boolean isOnline;
    @Getter
    private Optional<CompletableFuture<Boolean>> future = Optional.empty();

    public OnionServiceOnlineStateService(TorControlProtocol torControlProtocol, String onionAddress, long timeout) {
        super(EventType.HS_DESC, onionAddress, Set.of(HsDescEvent.Action.FAILED, HsDescEvent.Action.RECEIVED));

        this.torControlProtocol = torControlProtocol;
        this.onionAddress = onionAddress;
        this.timeout = timeout;
    }

    public CompletableFuture<Boolean> isOnionServiceOnline() {
        future = Optional.of(CompletableFuture.supplyAsync(() -> {
                    torControlProtocol.addHsDescEventListener(this);

                    String serviceId = onionAddress.replace(".onion", "");
                    torControlProtocol.hsFetch(serviceId);

                    boolean isSuccess;
                    try {
                        isSuccess = countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if (!isSuccess) {
                        throw new RuntimeException("Could not get onion address upload completed in " + timeout / 1000 + " seconds");
                    }

                    return isOnline;
                })
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
        switch (hsDescEvent.getAction()) {
            case FAILED:
                HsDescFailedEvent hsDescFailedEvent = (HsDescFailedEvent) hsDescEvent;
                if (hsDescFailedEvent.getReason().equals("REASON=NOT_FOUND")) {
                    log.info("hsFetch request for {} failed with reason NOT_FOUND", onionAddress);
                    countDownLatch.countDown();
                }
                break;
            case RECEIVED:
                log.info("hsFetch request for {} succeeded", onionAddress);
                isOnline = true;
                countDownLatch.countDown();
                break;
            case UPLOADED:
                break;
        }
    }
}
