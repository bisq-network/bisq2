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

package bisq.trade.protocol.messages;

import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Trade messages which arrived before the protocol of their trade existed. The trade service passes the messages of a
 * trade to its protocol when the protocol has handled a message of that trade, for a maker the take offer request.
 * <p>
 * Anybody can send a trade message with a made-up trade ID. Such a message never gets a protocol, so it would stay
 * in memory until the node restarts. The number of kept messages is therefore bounded by {@link #MAX_PENDING_MESSAGES}.
 * When the bound is reached, the oldest message is dropped: a message of a legitimate trade gets its protocol
 * shortly after it arrived, so an old pending message is most likely one which never gets a protocol.
 * <p>
 * Trade messages carry private data of the traders, for example payment account data. Users share their log files
 * with support, so the log statements name only the type and the trade ID of a message.
 */
@Slf4j
public class PendingTradeMessages<M extends TradeMessage> {
    public static final int MAX_PENDING_MESSAGES = 100;

    // Insertion order is the arrival order. All access is guarded by the monitor of this object.
    private final Set<M> messages = new LinkedHashSet<>();

    public synchronized void add(M message) {
        if (messages.contains(message)) {
            return;
        }
        if (messages.size() >= MAX_PENDING_MESSAGES) {
            Iterator<M> iterator = messages.iterator();
            M oldest = iterator.next();
            iterator.remove();
            log.warn("We drop the oldest pending trade message because the limit of {} pending messages is " +
                            "reached. type={}, tradeId={}",
                    MAX_PENDING_MESSAGES,
                    oldest.getClass().getSimpleName(),
                    StringUtils.sanitizeForLog(oldest.getTradeId()));
        }
        messages.add(message);
    }

    public synchronized boolean remove(M message) {
        return messages.remove(message);
    }

    public synchronized boolean isEmpty() {
        return messages.isEmpty();
    }

    public synchronized void clear() {
        messages.clear();
    }

    /**
     * A copy of the pending messages in arrival order. Later changes of the pending messages do not change the copy.
     */
    public synchronized List<M> getMessagesInArrivalOrder() {
        return new ArrayList<>(messages);
    }

    /**
     * Removes the pending messages of the trade and returns them in arrival order. Two callers never get the same
     * message, so the trade service passes each pending message to the protocol of its trade once.
     */
    public synchronized List<M> removeMessagesOfTrade(String tradeId) {
        List<M> messagesOfTrade = new ArrayList<>();
        Iterator<M> iterator = messages.iterator();
        while (iterator.hasNext()) {
            M message = iterator.next();
            if (message.getTradeId().equals(tradeId)) {
                messagesOfTrade.add(message);
                iterator.remove();
            }
        }
        return messagesOfTrade;
    }
}
