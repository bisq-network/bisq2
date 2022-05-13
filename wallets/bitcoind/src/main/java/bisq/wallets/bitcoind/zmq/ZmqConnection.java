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

package bisq.wallets.bitcoind.zmq;

import bisq.common.threading.ExecutorFactory;
import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.bitcoind.zmq.exceptions.CannotFindZmqAddressException;
import bisq.wallets.bitcoind.zmq.exceptions.CannotFindZmqTopicException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
public class ZmqConnection implements AutoCloseable {

    private static final int ERROR_CODE_SOCKET_CLOSED = 4;
    private static final int ERROR_CODE_CONTEXT_TERMINATED = 156384765;

    private final ZmqTopicProcessors topicProcessors;
    @Getter
    private final ZmqListeners listeners;

    private final ExecutorService executorService = ExecutorFactory
            .newFixedThreadPool("wallet-zeromq-notification-thread-pool", 2);

    private ZContext context;

    public ZmqConnection(ZmqTopicProcessors topicProcessors, ZmqListeners listeners) {
        this.topicProcessors = topicProcessors;
        this.listeners = listeners;
    }

    public void initialize(List<BitcoindGetZmqNotificationsResponse> zmqConnectionEndpoints) {
        context = new ZContext();
        executorService.execute(() -> {
            String zmqAddress = findZmqAddress(zmqConnectionEndpoints);
            ZMQ.Socket socket = createSocket(zmqAddress);
            try {
                messageLoop(socket);
            } catch (ZMQException e) {
                int errorCode = e.getErrorCode();
                if (!isSocketClosed(errorCode)) {
                    e.printStackTrace();
                }
            }
        });
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(this::close);
    }

    @Override
    public void close() {
        listeners.clearAll();
        executorService.shutdownNow();
        context.close();
    }

    private ZMQ.Socket createSocket(String zmqAddress) {
        ZMQ.Socket socket = context.createSocket(SocketType.SUB);

        // Subscribe to all topics
        Arrays.stream(BitcoindZmqTopic.values())
                .forEach(topic -> socket.subscribe(topic.getTopicName()));

        socket.connect(zmqAddress);
        return socket;
    }

    public void messageLoop(ZMQ.Socket socket) {
        while (!Thread.currentThread().isInterrupted()) {
            // Bitcoind ZeroMQ messages are made of 3 parts.
            String topicName = socket.recvStr();
            byte[] secondPart = socket.recv();
            byte[] thirdPart = socket.recv();

            executorService.execute(() -> {
                BitcoindZmqTopic zmqTopic = BitcoindZmqTopic.parse(topicName);
                var message = new BitcoindZmqMessage(zmqTopic, secondPart, thirdPart);
                topicProcessors.process(message);
            });
        }
    }

    private String findZmqAddress(List<BitcoindGetZmqNotificationsResponse> zmqNotifications) {
        if (!canSubscribeToAllTopics(zmqNotifications)) {
            throw new CannotFindZmqTopicException(
                    "ZeroMQ: Bitcoind hasn't publishing all topics (" +
                            Arrays.stream(BitcoindZmqTopic.values()).collect(Collectors.toSet()) +
                            ")"
            );
        }

        if (!allTopicsArePublishedToSameAddress(zmqNotifications)) {
            throw new CannotFindZmqAddressException("ZeroMQ: All topics need to published on the same address.");
        }

        return zmqNotifications.get(0).getAddress();
    }

    private boolean canSubscribeToAllTopics(List<BitcoindGetZmqNotificationsResponse> zmqNotifications) {
        Set<String> allTopicNames = Arrays.stream(BitcoindZmqTopic.values())
                .map(BitcoindZmqTopic::getTopicName)
                .collect(Collectors.toSet());

        long count = zmqNotifications.stream().map(BitcoindGetZmqNotificationsResponse::getType)
                .filter(allTopicNames::contains)
                .count();

        return count != allTopicNames.size();
    }

    private boolean allTopicsArePublishedToSameAddress(List<BitcoindGetZmqNotificationsResponse> zmqNotifications) {
        long count = zmqNotifications.stream()
                .map(BitcoindGetZmqNotificationsResponse::getAddress)
                .distinct()
                .count();
        return count == 1;
    }

    private boolean isSocketClosed(int errorCode) {
        return errorCode == ERROR_CODE_SOCKET_CLOSED || isZeroMqContextTerminated(errorCode);
    }

    private boolean isZeroMqContextTerminated(int errorCode) {
        return errorCode == ERROR_CODE_CONTEXT_TERMINATED;
    }
}
