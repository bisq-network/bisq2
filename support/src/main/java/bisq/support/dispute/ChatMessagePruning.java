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

package bisq.support.dispute;

import bisq.chat.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Selects the newest chat messages for a message to a dispute agent. The rules are specified in
 * docs/specifications/support/dispute-chat-history-pruning.md.
 */
@Slf4j
public final class ChatMessagePruning {
    private static final int MAX_TOTAL_CHAT_MESSAGES_TEXT_BYTES = 18_000;
    // Leave headroom for AEAD authentication tag (16 bytes) added by the
    // confidential transport on top of the serialized payload, so that the
    // resulting ciphertext stays within the transport's 20_000-byte limit.
    public static final int MAX_SERIALIZED_SIZE = 20_000 - 16;
    // The pruning keeps the newest messages, so it needs the chronological order. The callers pass the messages of a
    // chat channel, which keeps them in a set without a defined order. Messages with the same date are sorted by ID,
    // so that the selection does not depend on the order of the input.
    private static final Comparator<ChatMessage> CHRONOLOGICAL_ORDER =
            Comparator.comparingLong(ChatMessage::getDate).thenComparing(ChatMessage::getId);

    private ChatMessagePruning() {
    }

    /**
     * Creates the message with the newest chat messages which fit into the limits. The chat messages can be in any
     * order. The message factory receives the kept chat messages in chronological order. It throws
     * {@link SerializedSizeExceededException} if the message is too large.
     */
    public static <M extends ChatMessage, R> R createWithMaybePrunedMessages(List<M> chatMessages,
                                                                             String tradeId,
                                                                             Function<List<M>, R> messageFactory) {
        List<M> candidateMessages = pruneByTotalTextBytes(sortChronologically(chatMessages));
        while (true) {
            try {
                R message = messageFactory.apply(new ArrayList<>(candidateMessages));
                logIfPruned(chatMessages.size(), candidateMessages.size(), tradeId);
                return message;
            } catch (SerializedSizeExceededException e) {
                if (candidateMessages.isEmpty()) {
                    throw e;
                }
                // Removes the oldest message
                candidateMessages.remove(0);
            }
        }
    }

    private static <M extends ChatMessage> List<M> sortChronologically(List<M> chatMessages) {
        List<M> result = new ArrayList<>(chatMessages);
        result.sort(CHRONOLOGICAL_ORDER);
        return result;
    }

    private static <M extends ChatMessage> List<M> pruneByTotalTextBytes(List<M> chronologicalMessages) {
        int totalTextBytes = 0;
        List<M> result = new ArrayList<>();
        for (int i = chronologicalMessages.size() - 1; i >= 0; i--) {
            M message = chronologicalMessages.get(i);
            int messageTextBytes = message.getTextOrNA().getBytes(StandardCharsets.UTF_8).length;
            if (totalTextBytes + messageTextBytes >= MAX_TOTAL_CHAT_MESSAGES_TEXT_BYTES) {
                break;
            }
            totalTextBytes += messageTextBytes;
            result.add(message);
        }
        Collections.reverse(result);
        return result;
    }

    private static void logIfPruned(int originalSize,
                                    int resultSize,
                                    String tradeId) {
        if (resultSize != originalSize) {
            log.warn("chatMessages pruned for trade {}: kept={}, dropped={}, maxTotalTextBytes={}",
                    tradeId,
                    resultSize,
                    originalSize - resultSize,
                    MAX_TOTAL_CHAT_MESSAGES_TEXT_BYTES);
        }
    }

}
