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
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public final class ChatMessagePruning {
    public static final int MAX_TOTAL_CHAT_MESSAGES_TEXT_BYTES = 18_000;
    // Leave headroom for AEAD authentication tag (16 bytes) added by the
    // confidential transport on top of the serialized payload, so that the
    // resulting ciphertext stays within the transport's 20_000-byte limit.
    public static final int MAX_SERIALIZED_SIZE = 20_000 - 16;

    private ChatMessagePruning() {
    }

    public static <M extends ChatMessage> List<M> maybePrune(List<M> chatMessages,
                                                             int maxTotalTextBytes,
                                                             int maxSerializedSize,
                                                             ToIntFunction<List<M>> serializedSizeSupplier,
                                                             Logger log,
                                                             String tradeId) {
        int totalTextBytes = 0;
        List<M> result = new ArrayList<>();
        for (M message : chatMessages) {
            totalTextBytes += message.getTextOrNA().getBytes(StandardCharsets.UTF_8).length;
            if (totalTextBytes >= maxTotalTextBytes) {
                break;
            }
            result.add(message);
        }
        while (!result.isEmpty() && serializedSizeSupplier.applyAsInt(result) > maxSerializedSize) {
            result.removeLast();
        }
        if (result.size() != chatMessages.size()) {
            log.warn("chatMessages pruned for trade {}: kept={}, dropped={}, maxTotalTextBytes={}",
                    tradeId,
                    result.size(),
                    chatMessages.size() - result.size(),
                    maxTotalTextBytes);
        }
        return result;
    }
}
