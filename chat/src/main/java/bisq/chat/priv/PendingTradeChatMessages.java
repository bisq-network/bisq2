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

package bisq.chat.priv;

import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Chat messages of a trade whose trade channel does not exist yet. The chat service processes them again when the
 * channel is created.
 * <p>
 * Anybody can send chat messages for made-up trade IDs. Such a message never gets a channel, so it would stay in
 * memory until the node restarts. The number of kept messages is therefore bounded by {@link #MAX_PENDING_MESSAGES}.
 * When the bound is reached, the oldest message is dropped (rules 2 to 4 of
 * docs/specifications/trade/pending-trade-messages.md).
 * <p>
 * Chat messages carry private data of the traders. Users share their log files with support, so the log statements
 * name only the trade ID of a message.
 */
@Slf4j
public final class PendingTradeChatMessages<M extends PrivateChatMessage<?>> {
    public static final int MAX_PENDING_MESSAGES = 100;

    private final Function<M, String> tradeIdOfMessage;
    // Insertion order is the arrival order. All access is guarded by the monitor of this object.
    private final Set<M> messages = new LinkedHashSet<>();

    public PendingTradeChatMessages(Function<M, String> tradeIdOfMessage) {
        this.tradeIdOfMessage = tradeIdOfMessage;
    }

    public synchronized void add(M message) {
        if (messages.contains(message)) {
            return;
        }
        if (messages.size() >= MAX_PENDING_MESSAGES) {
            Iterator<M> iterator = messages.iterator();
            M oldest = iterator.next();
            iterator.remove();
            log.warn("We drop the oldest pending message because the limit of {} pending messages is reached. tradeId={}",
                    MAX_PENDING_MESSAGES, StringUtils.sanitizeForLog(tradeIdOfMessage.apply(oldest)));
        }
        messages.add(message);
        log.debug("We keep a message for a trade channel which does not exist as pending message. tradeId={}",
                StringUtils.sanitizeForLog(tradeIdOfMessage.apply(message)));
    }

    public synchronized boolean remove(M message) {
        return messages.remove(message);
    }

    public synchronized boolean isEmpty() {
        return messages.isEmpty();
    }

    public synchronized int size() {
        return messages.size();
    }

    /**
     * A copy of the pending messages in arrival order. The caller processes the messages outside the monitor of
     * this object, and processing a message may add or remove pending messages.
     */
    public synchronized List<M> getMessagesInArrivalOrder() {
        return new ArrayList<>(messages);
    }

    /**
     * A copy of the pending messages of the trade in arrival order, for the same reason as
     * {@link #getMessagesInArrivalOrder()}.
     */
    public synchronized List<M> getMessagesOfTrade(String tradeId) {
        return messages.stream()
                .filter(message -> tradeIdOfMessage.apply(message).equals(tradeId))
                .toList();
    }
}
