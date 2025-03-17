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

package bisq.network.p2p.node;

import bisq.common.util.MathUtils;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Checks if time between last send/receive is larger than the throttle time adjusted with the network load.
 * If so we pause the remaining time to ensure only 1 message is sent in that time slot.
 * This should avoid that a node gets flooded (at receive using the peers network load to adjust the throttle time) or
 * flooding their peers (at send using my network load to adjust the throttle time).
 * As larger value in the config for sendMessageTimestamp and receiveMessageTimestamp means it is more likely that
 * message send/receive get paused.
 * Messages are not dropped by we only pause the executing thread. We use lower and upper bounds for the pause, so
 * even with extreme values from the config the throttling should not have severe impacts on the
 * connection (e.g. lead to timeouts).
 */
@Slf4j
public class ConnectionThrottle {
    private static final long MIN_THROTTLE_TIME = 20;
    private static final long MAX_THROTTLE_TIME = 1000;
    private static final long MAX_LOG_FREQUENCY = TimeUnit.SECONDS.toMillis(30);

    // We apply the log throttle globally, so we use static fields
    private static final AtomicLong lastLoggedTs = new AtomicLong();
    private static final List<String> LAST_LOGS = new CopyOnWriteArrayList<>();

    private final NetworkLoadSnapshot peersNetworkLoadSnapshot;
    private final NetworkLoadSnapshot myNetworkLoadSnapshot;
    private final long sendMessageThrottleTime;
    private final long receiveMessageThrottleTime;
    private final AtomicLong sendMessageTimestamp = new AtomicLong();
    private final AtomicLong receiveMessageTimestamp = new AtomicLong();

    public ConnectionThrottle(NetworkLoadSnapshot peersNetworkLoadSnapshot,
                              NetworkLoadSnapshot myNetworkLoadSnapshot,
                              Node.Config config) {
        this.peersNetworkLoadSnapshot = peersNetworkLoadSnapshot;
        this.myNetworkLoadSnapshot = myNetworkLoadSnapshot;
        sendMessageThrottleTime = config.getSendMessageThrottleTime(); // default 200
        receiveMessageThrottleTime = config.getReceiveMessageThrottleTime(); // default 200
    }

    void throttleSendMessage() {
        throttle(sendMessageTimestamp, peersNetworkLoadSnapshot, sendMessageThrottleTime, "send");
    }

    void throttleReceiveMessage() {
        throttle(receiveMessageTimestamp, myNetworkLoadSnapshot, receiveMessageThrottleTime, "receive");
    }

    private void throttle(AtomicLong timestamp, NetworkLoadSnapshot networkLoadSnapshot, long throttleTime, String direction) {
        long now = System.currentTimeMillis();
        long passed = now - timestamp.get();
        double load = networkLoadSnapshot.getCurrentNetworkLoad().getLoad();
        throttleTime = MIN_THROTTLE_TIME + Math.round(throttleTime * load);
        throttleTime = MathUtils.bounded(MIN_THROTTLE_TIME, MAX_THROTTLE_TIME, throttleTime);
        if (passed < throttleTime) {
            try {
                long pause = throttleTime - passed;
                pause = MathUtils.bounded(1, MAX_THROTTLE_TIME, pause);
                String logMessage = String.format("Pause '%s' message for %d ms. Network=%f", direction, pause, load);
                long passedSinceLastLog = now - lastLoggedTs.get();
                if (passedSinceLastLog < MAX_LOG_FREQUENCY) {
                    LAST_LOGS.add(logMessage);
                    if (lastLoggedTs.get() == 0) {
                        lastLoggedTs.set(now);
                    }
                } else {
                    if (LAST_LOGS.isEmpty()) {
                        log.info(logMessage);
                    } else {
                        LAST_LOGS.add(logMessage);
                        List<String> temp = new ArrayList<>(LAST_LOGS);
                        int size = temp.size();
                        List<String> subList = temp.subList(0, Math.min(5, size));
                        log.info("{} accumulated log messages in the past {} sec. Log message (max 5 displayed): {}",
                                size, passedSinceLastLog / 1000, subList);
                        LAST_LOGS.clear();
                    }
                    lastLoggedTs.set(now);
                }
                Thread.sleep(pause);
            } catch (InterruptedException ignore) {
            }
        }
        timestamp.set(now);
    }
}
