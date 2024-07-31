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

import bisq.tor.controller.events.events.HsDescEvent;
import bisq.tor.controller.events.events.HsDescFailedEvent;
import bisq.tor.controller.events.listener.HsDescEventListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class OnionServiceOnlineStateService implements HsDescEventListener {
    private final TorControlProtocol torControlProtocol;
    private final String onionAddress;
    @Getter
    private final CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

    public OnionServiceOnlineStateService(TorControlProtocol torControlProtocol, String onionAddress) {
        this.torControlProtocol = torControlProtocol;
        this.onionAddress = onionAddress;
    }

    public CompletableFuture<Boolean> isOnionServiceOnline() {
        subscribeToHsDescEvents();

        String serviceId = onionAddress.replace(".onion", "");
        torControlProtocol.hsFetch(serviceId);

        completableFuture.thenRun(() -> {
            torControlProtocol.removeHsDescEventListener(this);
            torControlProtocol.setEvents(Collections.emptyList());
        });

        return completableFuture;
    }

    @Override
    public void onHsDescEvent(HsDescEvent hsDescEvent) {
        log.info("Tor HS_DESC event: {}", hsDescEvent);

        switch (hsDescEvent.getAction()) {
            case FAILED:
                HsDescFailedEvent hsDescFailedEvent = (HsDescFailedEvent) hsDescEvent;
                if (hsDescFailedEvent.getReason().equals("REASON=NOT_FOUND")) {
                    completableFuture.complete(false);
                }
                break;
            case RECEIVED:
                completableFuture.complete(true);
                break;
            case UPLOADED:
                break;
        }
    }

    private void subscribeToHsDescEvents() {
        torControlProtocol.addHsDescEventListener(this);
        torControlProtocol.setEvents(List.of("HS_DESC"));
    }
}
