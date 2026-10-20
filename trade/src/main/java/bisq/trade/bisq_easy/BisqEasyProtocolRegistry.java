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

package bisq.trade.bisq_easy;

import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;
import bisq.trade.protocol.messages.PendingTradeMessages;
import com.google.common.annotations.VisibleForTesting;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The protocols of the Bisq Easy trades, and the trade messages whose trade has no protocol yet.
 * <p>
 * Rule 1 of docs/specifications/trade/pending-trade-messages.md makes the decision "no protocol, so the message is
 * pending" and the registration of a protocol one step. So the lookup and the add of a pending message run under the
 * same lock as every change of the protocols. A message which arrives while a take offer request creates the protocol
 * of its trade then either finds the protocol, or is pending before the request processes the pending messages. The
 * network layer does not deliver the message again, so a message which became pending after that pass would not reach
 * the protocol.
 * <p>
 * Under the lock, the registry only changes its own data. The only other lock which it takes there is the monitor of
 * the pending messages, and that monitor takes no further lock. So the lock cannot be part of a lock cycle. The trade
 * service handles the messages and calls the protocols outside of the lock.
 */
final class BisqEasyProtocolRegistry {
    private final Object lock = new Object();
    // We don't persist the protocol, only the model.
    private final Map<String, BisqEasyProtocol> protocolByTradeId = new ConcurrentHashMap<>();
    private final PendingTradeMessages<BisqEasyTradeMessage> pendingMessages;

    BisqEasyProtocolRegistry() {
        this(new PendingTradeMessages<>());
    }

    @VisibleForTesting
    BisqEasyProtocolRegistry(PendingTradeMessages<BisqEasyTradeMessage> pendingMessages) {
        this.pendingMessages = pendingMessages;
    }

    // A lookup alone decides nothing about the pending messages, so it needs no lock
    Optional<BisqEasyProtocol> find(String tradeId) {
        return Optional.ofNullable(protocolByTradeId.get(tradeId));
    }

    // A copy, so that later changes of the protocols do not change it
    List<BisqEasyProtocol> getProtocols() {
        return List.copyOf(protocolByTradeId.values());
    }

    /**
     * Returns the protocol of the trade of the message. If the trade has no protocol, the message is pending before
     * this method returns, and the result is empty.
     */
    Optional<BisqEasyProtocol> findOrKeepAsPending(BisqEasyTradeMessage message) {
        synchronized (lock) {
            // The lookup goes through find, so a test can stop a thread between the lookup and the decision
            Optional<BisqEasyProtocol> protocol = find(message.getTradeId());
            if (protocol.isEmpty()) {
                pendingMessages.add(message);
            }
            return protocol;
        }
    }

    void add(String tradeId, BisqEasyProtocol protocol) {
        synchronized (lock) {
            protocolByTradeId.put(tradeId, protocol);
        }
    }

    void remove(String tradeId) {
        synchronized (lock) {
            protocolByTradeId.remove(tradeId);
        }
    }

    /**
     * Removes the pending messages of the trade and returns them in arrival order. Two callers never get the same
     * message, so each pending message reaches the protocol of its trade once.
     * <p>
     * Call it only after {@link #add(String, BisqEasyProtocol)} registered the protocol of the trade. A message becomes
     * pending only while its trade has no protocol, because {@link #findOrKeepAsPending(BisqEasyTradeMessage)} decides
     * under the lock of the registration. So a call after the registration gets every pending message of the trade. A
     * call before it could miss a message which becomes pending after the call. A taker and the startup register
     * protocols without such a call: the peer of a taker learns the trade ID only from the take offer request, which
     * the taker sends after the registration, and at startup the node registers the protocols of its stored trades
     * before it subscribes to messages (rule 1 of docs/specifications/trade/pending-trade-messages.md).
     */
    List<BisqEasyTradeMessage> removePendingMessagesOfTrade(String tradeId) {
        return pendingMessages.removeMessagesOfTrade(tradeId);
    }

    @VisibleForTesting
    boolean hasPendingMessages() {
        return !pendingMessages.isEmpty();
    }

    /**
     * A copy of the pending messages in arrival order. Later changes of the pending messages do not change the copy.
     */
    @VisibleForTesting
    List<BisqEasyTradeMessage> getPendingMessagesInArrivalOrder() {
        return pendingMessages.getMessagesInArrivalOrder();
    }
}
