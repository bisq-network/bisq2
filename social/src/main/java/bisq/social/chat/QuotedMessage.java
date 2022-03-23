package bisq.social.chat;

import bisq.common.data.ByteArray;
import bisq.common.encoding.Proto;

public record QuotedMessage(String userName, ByteArray pubKeyHash, String message) implements Proto {
}
