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
import java.util.List;
import java.util.function.Function;

@Slf4j
public final class ChatMessagePruning {
    private static final int MAX_TOTAL_CHAT_MESSAGES_TEXT_BYTES = 18_000;
    // Leave headroom for AEAD authentication tag (16 bytes) added by the
    // confidential transport on top of the serialized payload, so that the
    // resulting ciphertext stays within the transport's 20_000-byte limit.
    public static final int MAX_SERIALIZED_SIZE = 20_000 - 16;

    private ChatMessagePruning() {
    }

    public static <M extends ChatMessage, R> R createWithMaybePrunedMessages(List<M> chatMessages,
                                                                             String tradeId,
                                                                             Function<List<M>, R> messageFactory) {
        List<M> candidateMessages = pruneByTotalTextBytes(chatMessages);
        while (true) {
            try {
                R message = messageFactory.apply(new ArrayList<>(candidateMessages));
                logIfPruned(chatMessages.size(), candidateMessages.size(), tradeId);
                return message;
            } catch (SerializedSizeExceededException e) {
                if (candidateMessages.isEmpty()) {
                    throw e;
                }
                candidateMessages.remove(0);
            }
        }
    }

    private static <M extends ChatMessage> List<M> pruneByTotalTextBytes(List<M> chatMessages) {
        int totalTextBytes = 0;
        List<M> result = new ArrayList<>();
        for (int i = chatMessages.size() - 1; i >= 0; i--) {
            M message = chatMessages.get(i);
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
