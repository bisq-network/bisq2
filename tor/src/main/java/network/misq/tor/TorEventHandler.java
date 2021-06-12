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

package network.misq.tor;

import net.freehaven.tor.control.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TorEventHandler implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(TorEventHandler.class);

    private final Map<String, Runnable> hiddenServiceReadyListeners = new ConcurrentHashMap<>();

    public TorEventHandler() {
    }

    public void addHiddenServiceReadyListener(String serviceId, Runnable listener) {
        hiddenServiceReadyListeners.put(serviceId, listener);
    }

    public void removeHiddenServiceReadyListener(String serviceId) {
        hiddenServiceReadyListeners.remove(serviceId);
    }

    @Override
    public void circuitStatus(String status, String id, String path) {
        log.debug("circuitStatus {} {} {}", status, id, path);
    }

    @Override
    public void streamStatus(String status, String id, String target) {
        log.debug("streamStatus {} {}", status, id);
    }

    @Override
    public void orConnStatus(String status, String orName) {
        log.debug("orConnStatus {} {}", status, orName);
    }

    @Override
    public void bandwidthUsed(long read, long written) {
        log.debug("bandwidthUsed {} {}", read, written);
    }

    @Override
    public void newDescriptors(List<String> orList) {
        log.debug("newDescriptors {}", orList);
    }

    @Override
    public void message(String severity, String msg) {
        log.debug("message {} {}", severity, msg);
    }

    @Override
    public void hiddenServiceEvent(String action, String msg) {
        log.debug("hiddenServiceEvent action: {} msg:{}", action, msg);
        if (!hiddenServiceReadyListeners.isEmpty() && action.equals("UPLOADED")) {
            Optional<String> optional = hiddenServiceReadyListeners.entrySet().stream()
                    .filter(entry -> msg.contains(entry.getKey()))
                    .peek(entry -> entry.getValue().run())
                    .map(Map.Entry::getKey)
                    .findAny();
            optional.ifPresent(this::removeHiddenServiceReadyListener);
        }
    }

    @Override
    public void hiddenServiceFailedEvent(String reason, String msg) {
        log.debug("hiddenServiceFailedEvent {} {}", reason, msg);
    }

    @Override
    public void hiddenServiceDescriptor(String descriptorId, String descriptor, String msg) {
        log.debug("hiddenServiceDescriptor {} {} {}", descriptorId, descriptor, msg);
    }

    @Override
    public void unrecognized(String type, String msg) {
        log.debug("unrecognized {} {}", type, msg);
    }

    @Override
    public void timeout() {
        log.debug("timeout");
    }
}
