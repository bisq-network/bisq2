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

import bisq.security.keys.TorKeyPair;
import bisq.tor.controller.events.events.HsDescEvent;
import bisq.tor.controller.events.listener.HsDescEventListener;
import bisq.tor.controller.exceptions.HsDescUploadFailedException;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PublishOnionAddressService implements HsDescEventListener {
    private final TorControlProtocol torControlProtocol;
    private final int hsUploadTimeout;
    private final TorKeyPair torKeyPair;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public PublishOnionAddressService(TorControlProtocol torControlProtocol, int hsUploadTimeout, TorKeyPair torKeyPair) {
        super();
        this.torControlProtocol = torControlProtocol;
        this.hsUploadTimeout = hsUploadTimeout;
        this.torKeyPair = torKeyPair;
    }

    public void publish(int onionServicePort, int localPort) throws InterruptedException {
        subscribeToHsDescEvents();
        torControlProtocol.addOnion(torKeyPair, onionServicePort, localPort);

        boolean isSuccess = countDownLatch.await(hsUploadTimeout, TimeUnit.MILLISECONDS);
        if (!isSuccess) {
            throw new HsDescUploadFailedException("HS_DESC upload timer triggered.");
        }

        torControlProtocol.removeHsDescEventListener(this);
        torControlProtocol.setEvents(Collections.emptyList());
    }

    @Override
    public void onHsDescEvent(HsDescEvent hsDescEvent) {
        log.info("Tor HS_DESC event: {}", hsDescEvent);

        switch (hsDescEvent.getAction()) {
            case FAILED:
                break;
            case RECEIVED:
                break;
            case UPLOADED:
                countDownLatch.countDown();
                break;
        }
    }

    private void subscribeToHsDescEvents() {
        torControlProtocol.addHsDescEventListener(this);
        torControlProtocol.setEvents(List.of("HS_DESC"));
    }
}
