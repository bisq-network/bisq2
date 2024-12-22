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

package bisq.http_api.web_socket.subscription;

import bisq.common.threading.ExecutorFactory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
@EqualsAndHashCode
public class Subscriber {
    private final Topic topic;
    private final Optional<String> parameter;
    private final String subscriberId;
    private final WebSocket webSocket;
    private final AtomicInteger sequenceNumber = new AtomicInteger(0); // sequenceNumber start with 0 at subscribe time and gets increased at each emitted WebSocketEvent
    private final ExecutorService executorService;

    public Subscriber(Topic topic,
                      Optional<String> parameter,
                      String subscriberId,
                      WebSocket webSocket) {
        this.topic = topic;
        this.parameter = parameter;
        this.subscriberId = subscriberId;
        this.webSocket = webSocket;
        executorService = ExecutorFactory.newSingleThreadExecutor("Subscriber-" + topic.name() + "-" + subscriberId);
    }

    public int incrementAndGetSequenceNumber() {
        return sequenceNumber.incrementAndGet();
    }

    public CompletableFuture<Boolean> send(String json) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DataFrame dataFrame = webSocket.send(json).get();
                return true;
            } catch (Exception e) {
                log.error("Sending webSocketEvent failed", e);
                return false;
            }
        }, executorService);
    }
}
